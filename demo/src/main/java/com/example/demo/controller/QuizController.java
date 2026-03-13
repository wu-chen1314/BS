package com.example.demo.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.QuizAttempt;
import com.example.demo.entity.QuizQuestion;
import com.example.demo.request.QuizAttemptSubmitRequest;
import com.example.demo.request.QuizQuestionUpsertRequest;
import com.example.demo.service.QuizAttemptService;
import com.example.demo.service.QuizQuestionService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizQuestionService quizQuestionService;
    private final QuizAttemptService quizAttemptService;
    private final RequestAuthUtil requestAuthUtil;

    public QuizController(QuizQuestionService quizQuestionService,
                          QuizAttemptService quizAttemptService,
                          RequestAuthUtil requestAuthUtil) {
        this.quizQuestionService = quizQuestionService;
        this.quizAttemptService = quizAttemptService;
        this.requestAuthUtil = requestAuthUtil;
    }

    @GetMapping("/questions")
    public Result<List<Map<String, Object>>> getQuestions(@RequestParam(defaultValue = "5") Integer limit) {
        int safeLimit = Math.max(1, Math.min(limit == null ? 5 : limit, 10));
        List<QuizQuestion> questions = quizQuestionService.list(new LambdaQueryWrapper<QuizQuestion>()
                .eq(QuizQuestion::getActive, true)
                .orderByAsc(QuizQuestion::getSortOrder)
                .orderByAsc(QuizQuestion::getId));

        if (questions.isEmpty()) {
            return Result.error("当前暂无可用题目");
        }

        List<QuizQuestion> randomized = new ArrayList<>(questions);
        Collections.shuffle(randomized);
        return Result.success(randomized.stream()
                .limit(safeLimit)
                .map(this::toQuestionPayload)
                .collect(Collectors.toList()));
    }

    @GetMapping("/questions/manage")
    public Result<Map<String, Object>> listManagedQuestions(@RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "8") Integer pageSize,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Boolean active,
                                                            HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("只有管理员可以管理题库");
        }

        int safePageNum = Math.max(1, pageNum == null ? 1 : pageNum);
        int safePageSize = Math.max(1, Math.min(pageSize == null ? 8 : pageSize, 30));

        LambdaQueryWrapper<QuizQuestion> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String safeKeyword = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(QuizQuestion::getQuestionText, safeKeyword)
                    .or()
                    .like(QuizQuestion::getCategory, safeKeyword)
                    .or()
                    .like(QuizQuestion::getExplanation, safeKeyword));
        }
        if (active != null) {
            query.eq(QuizQuestion::getActive, active);
        }
        query.orderByAsc(QuizQuestion::getSortOrder).orderByAsc(QuizQuestion::getId);

        Page<QuizQuestion> page = quizQuestionService.page(new Page<>(safePageNum, safePageSize), query);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", page.getRecords().stream().map(this::toManagedQuestionPayload).collect(Collectors.toList()));
        result.put("total", page.getTotal());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return Result.success(result);
    }

    @PostMapping("/questions/manage")
    public Result<Map<String, Object>> createQuestion(@RequestBody QuizQuestionUpsertRequest requestBody,
                                                      HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("只有管理员可以管理题库");
        }

        Result<QuizQuestion> normalizedResult = normalizeQuestion(null, requestBody);
        if (normalizedResult.getCode() != 200) {
            return Result.error(normalizedResult.getMsg());
        }

        QuizQuestion question = normalizedResult.getData();
        quizQuestionService.save(question);
        return Result.success(toManagedQuestionPayload(question));
    }

    @PutMapping("/questions/manage/{id}")
    public Result<Map<String, Object>> updateQuestion(@PathVariable Long id,
                                                      @RequestBody QuizQuestionUpsertRequest requestBody,
                                                      HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("只有管理员可以管理题库");
        }

        QuizQuestion existing = quizQuestionService.getById(id);
        if (existing == null) {
            return Result.error("题目不存在");
        }

        Result<QuizQuestion> normalizedResult = normalizeQuestion(existing, requestBody);
        if (normalizedResult.getCode() != 200) {
            return Result.error(normalizedResult.getMsg());
        }

        QuizQuestion question = normalizedResult.getData();
        question.setId(id);
        quizQuestionService.updateById(question);
        return Result.success(toManagedQuestionPayload(question));
    }

    @DeleteMapping("/questions/manage/{id}")
    public Result<Boolean> deleteQuestion(@PathVariable Long id, HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("只有管理员可以管理题库");
        }

        QuizQuestion existing = quizQuestionService.getById(id);
        if (existing == null) {
            return Result.error("题目不存在");
        }

        return Result.success(quizQuestionService.removeById(id));
    }

    @PostMapping("/attempts")
    public Result<Map<String, Object>> submitAttempt(@RequestBody QuizAttemptSubmitRequest requestBody,
                                                     HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未授权，请先登录");
        }
        if (requestBody == null || requestBody.getAnswers() == null || requestBody.getAnswers().isEmpty()) {
            return Result.error("答题结果不能为空");
        }

        List<Long> questionIds = requestBody.getAnswers().stream()
                .map(QuizAttemptSubmitRequest.QuizAnswerItem::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (questionIds.isEmpty()) {
            return Result.error("题目编号不能为空");
        }

        Map<Long, QuizQuestion> questionMap = quizQuestionService.listByIds(questionIds).stream()
                .collect(Collectors.toMap(QuizQuestion::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        if (questionMap.isEmpty()) {
            return Result.error("未找到有效题目");
        }

        int totalQuestions = questionMap.size();
        int correctCount = 0;
        List<Map<String, Object>> answerResults = new ArrayList<>();

        for (QuizAttemptSubmitRequest.QuizAnswerItem item : requestBody.getAnswers()) {
            if (item.getQuestionId() == null) {
                continue;
            }
            QuizQuestion question = questionMap.get(item.getQuestionId());
            if (question == null) {
                continue;
            }
            boolean correct = item.getSelectedIndex() != null && item.getSelectedIndex().equals(question.getCorrectIndex());
            if (correct) {
                correctCount++;
            }

            Map<String, Object> answerResult = new LinkedHashMap<>();
            answerResult.put("questionId", question.getId());
            answerResult.put("question", question.getQuestionText());
            answerResult.put("selectedIndex", item.getSelectedIndex());
            answerResult.put("correctIndex", question.getCorrectIndex());
            answerResult.put("correct", correct);
            answerResult.put("category", question.getCategory());
            answerResult.put("explanation", question.getExplanation());
            answerResults.add(answerResult);
        }

        int score = (int) Math.round(correctCount * 100.0 / Math.max(1, totalQuestions));
        QuizAttempt attempt = new QuizAttempt();
        attempt.setUserId(userId);
        attempt.setScore(score);
        attempt.setCorrectCount(correctCount);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setDurationSeconds(Math.max(0, requestBody.getDurationSeconds() == null ? 0 : requestBody.getDurationSeconds()));
        attempt.setAnswerDetailsJson(JSONUtil.toJsonStr(answerResults));
        attempt.setCreatedAt(LocalDateTime.now());
        quizAttemptService.save(attempt);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attemptId", attempt.getId());
        result.put("score", score);
        result.put("correctCount", correctCount);
        result.put("totalQuestions", totalQuestions);
        result.put("durationSeconds", attempt.getDurationSeconds());
        result.put("submittedAt", attempt.getCreatedAt());
        result.put("answerResults", answerResults);
        return Result.success(result);
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary(HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未授权，请先登录");
        }
        return Result.success(buildSummary(userId));
    }

    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getHistory(@RequestParam(defaultValue = "6") Integer limit,
                                                        HttpServletRequest request) {
        Long userId = requestAuthUtil.getCurrentUserId(request);
        if (userId == null) {
            return Result.error("未授权，请先登录");
        }

        int safeLimit = Math.max(1, Math.min(limit == null ? 6 : limit, 20));
        Page<QuizAttempt> page = quizAttemptService.page(new Page<>(1, safeLimit),
                new LambdaQueryWrapper<QuizAttempt>()
                        .eq(QuizAttempt::getUserId, userId)
                        .orderByDesc(QuizAttempt::getCreatedAt)
                        .orderByDesc(QuizAttempt::getId));

        return Result.success(page.getRecords().stream()
                .map(this::toAttemptPayload)
                .collect(Collectors.toList()));
    }

    private Result<QuizQuestion> normalizeQuestion(QuizQuestion existing, QuizQuestionUpsertRequest requestBody) {
        if (requestBody == null) {
            return Result.error("题目内容不能为空");
        }

        String questionText = trimToNull(requestBody.getQuestion());
        String explanation = trimToNull(requestBody.getExplanation());
        List<String> normalizedOptions = requestBody.getOptions() == null
                ? Collections.emptyList()
                : requestBody.getOptions().stream()
                .map(this::trimToNull)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        if (!StringUtils.hasText(questionText)) {
            return Result.error("请输入题目内容");
        }
        if (normalizedOptions.size() < 2) {
            return Result.error("请至少提供两个有效选项");
        }
        if (!StringUtils.hasText(explanation)) {
            return Result.error("请输入题目解析");
        }
        if (requestBody.getCorrectAnswer() == null
                || requestBody.getCorrectAnswer() < 0
                || requestBody.getCorrectAnswer() >= normalizedOptions.size()) {
            return Result.error("正确答案选项无效");
        }

        LocalDateTime now = LocalDateTime.now();
        QuizQuestion question = existing == null ? new QuizQuestion() : existing;
        question.setQuestionText(questionText);
        question.setOptionsJson(JSONUtil.toJsonStr(normalizedOptions));
        question.setCorrectIndex(requestBody.getCorrectAnswer());
        question.setExplanation(explanation);
        question.setCategory(defaultIfBlank(requestBody.getCategory(), "综合"));
        question.setDifficulty(defaultIfBlank(requestBody.getDifficulty(), "medium"));
        question.setSortOrder(requestBody.getSortOrder() == null ? 0 : Math.max(0, requestBody.getSortOrder()));
        question.setActive(requestBody.getActive() == null ? Boolean.TRUE : requestBody.getActive());
        question.setUpdatedAt(now);
        question.setCreatedAt(existing == null || existing.getCreatedAt() == null ? now : existing.getCreatedAt());
        return Result.success(question);
    }

    private Map<String, Object> buildSummary(Long userId) {
        List<QuizAttempt> attempts = quizAttemptService.list(new LambdaQueryWrapper<QuizAttempt>()
                .eq(QuizAttempt::getUserId, userId)
                .orderByDesc(QuizAttempt::getCreatedAt)
                .orderByDesc(QuizAttempt::getId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attemptCount", attempts.size());

        if (attempts.isEmpty()) {
            result.put("bestScore", 0);
            result.put("averageScore", 0);
            result.put("latestScore", null);
            result.put("latestCorrectCount", null);
            result.put("latestAttemptAt", null);
            result.put("consistency", "尚未开始答题");
            return result;
        }

        int bestScore = attempts.stream().map(QuizAttempt::getScore).filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
        int averageScore = (int) Math.round(attempts.stream()
                .map(QuizAttempt::getScore)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0));
        QuizAttempt latest = attempts.get(0);

        result.put("bestScore", bestScore);
        result.put("averageScore", averageScore);
        result.put("latestScore", latest.getScore());
        result.put("latestCorrectCount", latest.getCorrectCount());
        result.put("latestAttemptAt", latest.getCreatedAt());
        result.put("consistency", buildConsistencyLabel(averageScore, attempts.size()));
        return result;
    }

    private String buildConsistencyLabel(int averageScore, int attemptCount) {
        if (attemptCount < 2) {
            return "先完成几次挑战，再看稳定度走势";
        }
        if (averageScore >= 85) {
            return "高稳定度，知识掌握已经比较扎实";
        }
        if (averageScore >= 65) {
            return "整体稳定，继续刷题能进一步拉高上限";
        }
        return "基础还在建立阶段，建议先结合资讯和项目内容复习";
    }

    private Map<String, Object> toQuestionPayload(QuizQuestion question) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", question.getId());
        result.put("question", question.getQuestionText());
        result.put("options", JSONUtil.toList(question.getOptionsJson(), String.class));
        result.put("correctAnswer", question.getCorrectIndex());
        result.put("explanation", question.getExplanation());
        result.put("category", StringUtils.hasText(question.getCategory()) ? question.getCategory().trim() : null);
        result.put("difficulty", StringUtils.hasText(question.getDifficulty()) ? question.getDifficulty().trim() : null);
        return result;
    }

    private Map<String, Object> toManagedQuestionPayload(QuizQuestion question) {
        Map<String, Object> result = new LinkedHashMap<>(toQuestionPayload(question));
        result.put("sortOrder", question.getSortOrder() == null ? 0 : question.getSortOrder());
        result.put("active", Boolean.TRUE.equals(question.getActive()));
        result.put("createdAt", question.getCreatedAt());
        result.put("updatedAt", question.getUpdatedAt());
        return result;
    }

    private Map<String, Object> toAttemptPayload(QuizAttempt attempt) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", attempt.getId());
        result.put("score", attempt.getScore());
        result.put("correctCount", attempt.getCorrectCount());
        result.put("totalQuestions", attempt.getTotalQuestions());
        result.put("durationSeconds", attempt.getDurationSeconds());
        result.put("createdAt", attempt.getCreatedAt());
        return result;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }
}
