package com.example.demo.config;

import cn.hutool.json.JSONUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class ContentEngagementSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public ContentEngagementSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        createNewsTables();
        createQuizTables();
        seedNewsArticles();
        seedQuizQuestions();
    }

    private void createNewsTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS news_article (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "title VARCHAR(255) NOT NULL," +
                "summary VARCHAR(500) NOT NULL," +
                "content TEXT NOT NULL," +
                "source VARCHAR(120) NOT NULL," +
                "tag VARCHAR(64) NOT NULL," +
                "tag_type VARCHAR(32) NULL," +
                "cover_image_url VARCHAR(500) NULL," +
                "video_url VARCHAR(500) NULL," +
                "published_at DATETIME NOT NULL," +
                "view_count INT NOT NULL DEFAULT 0," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "KEY idx_news_article_published (published_at)," +
                "KEY idx_news_article_tag (tag)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        ensureColumnExists("news_article", "cover_image_url",
                "ALTER TABLE news_article ADD COLUMN cover_image_url VARCHAR(500) NULL");
        ensureColumnExists("news_article", "video_url",
                "ALTER TABLE news_article ADD COLUMN video_url VARCHAR(500) NULL");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS news_read_log (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL," +
                "article_id BIGINT NOT NULL," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "KEY idx_news_read_log_user_created (user_id, created_at)," +
                "KEY idx_news_read_log_article_created (article_id, created_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void createQuizTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS quiz_question (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "question_text VARCHAR(500) NOT NULL," +
                "options_json TEXT NOT NULL," +
                "correct_index INT NOT NULL," +
                "explanation TEXT NOT NULL," +
                "category VARCHAR(64) NULL," +
                "difficulty VARCHAR(32) NULL," +
                "sort_order INT NOT NULL DEFAULT 0," +
                "active TINYINT(1) NOT NULL DEFAULT 1," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "KEY idx_quiz_question_sort (sort_order)," +
                "KEY idx_quiz_question_active (active)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS quiz_attempt (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL," +
                "score INT NOT NULL," +
                "correct_count INT NOT NULL," +
                "total_questions INT NOT NULL," +
                "duration_seconds INT NULL DEFAULT 0," +
                "answer_details_json TEXT NULL," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "KEY idx_quiz_attempt_user_created (user_id, created_at)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void seedNewsArticles() {
        Long articleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM news_article", Long.class);
        if (articleCount != null && articleCount > 0) {
            return;
        }

        insertNewsArticle(
                "关于进一步加强非物质文化遗产保护工作的指导意见发布",
                "文化和旅游领域发布新一轮指导意见，强调以数字化、活态传承和公众参与为抓手，推动非遗保护从资料留存走向场景再生。",
                "近日，相关主管部门发布新一轮非遗保护指导意见，提出要进一步完善代表性项目数据库、传承人支持机制和社会参与路径。\n\n" +
                        "文件明确提出，数字化不应只停留在图片采集和档案留存，还应服务于课程开发、路线策划、公共传播和青年参与。对地方项目来说，重点是把“资源可见”升级为“内容可用”。\n\n" +
                        "对于当前平台类产品，这意味着资讯、项目、路线和学习模块之间需要形成可追踪的数据闭环，让一次内容曝光能够真正带动后续学习、收藏和线下体验。",
                "文化和旅游部",
                "政策导向",
                "danger",
                "https://images.unsplash.com/photo-1590059940251-246ebec5896a?w=800&auto=format&fit=crop&q=60",
                "2026-03-12 09:00:00"
        );
        insertNewsArticle(
                "“巧夺天工”传统工艺振兴成果展将在北京启幕",
                "展览将集中展示刺绣、陶瓷、木作、漆艺等多个工艺门类的代表性作品，并同步开放面向青少年的互动体验日。",
                "本次成果展由多家文化机构联合发起，重点呈现传统工艺在当代设计、教育传播和文旅融合中的新场景。\n\n" +
                        "策展团队特别设置了“工艺如何进入现代生活”专题单元，观众不仅能看到代表作品，也能了解它们如何转化为课程、文创和社区工作坊。\n\n" +
                        "这类展讯资讯适合作为平台内容分发入口，与路线规划和研学工坊形成联动，提升用户连续浏览和转化效果。",
                "国家博物馆",
                "文化展览",
                "warning",
                "https://images.unsplash.com/photo-1614742517604-586fc2c918ef?w=800&auto=format&fit=crop&q=60",
                "2026-03-08 10:30:00"
        );
        insertNewsArticle(
                "数字技术如何赋能古老非遗：AI 与影像采集进入传承现场",
                "在皮影、木偶、戏曲等项目中，动作捕捉与影像重建正在帮助传统技艺获得更适合传播的新表达。",
                "随着内容生产方式的变化，越来越多的非遗项目开始尝试使用数字影像、动作采集和 AI 辅助整理来沉淀知识。\n\n" +
                        "对平台侧来说，这类内容不应只是单篇资讯，而应成为带动项目详情、学习清单和问答助手的“知识节点”。\n\n" +
                        "当用户阅读资讯后，系统若能继续推荐相关项目和学习主题，就能显著减少信息断层和跳出。",
                "数字传承专栏",
                "传承视点",
                "success",
                "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=800&auto=format&fit=crop&q=60",
                "2026-03-01 15:00:00"
        );
        insertNewsArticle(
                "2026 年“文化和自然遗产日”主题活动安排公布",
                "各地将围绕社区参与、学校传播和非遗体验空间建设展开联动，线上线下同步组织展示活动。",
                "今年的主题活动强调把遗产保护与公众教育结合起来，尤其鼓励博物馆、校园和街区共同打造面向年轻人的轻量体验。\n\n" +
                        "从运营角度看，这意味着平台资讯模块不只是新闻列表，更应该承担活动导流、内容说明和用户触达的作用。\n\n" +
                        "如果后续能够继续接入报名或收藏能力，资讯模块会成为很强的用户留存入口。",
                "中国非遗日组委会",
                "活动预告",
                "info",
                "https://plus.unsplash.com/premium_photo-1661882065870-fb97bcbe7142?w=800&auto=format&fit=crop&q=60",
                "2026-02-25 08:00:00"
        );
        insertNewsArticle(
                "地方戏曲进校园实践升温，课程设计更重“可参与”",
                "多地开始以短课、工作坊和社团项目替代一次性讲座，让学生在体验中建立对非遗的真实感知。",
                "相较于过去单向输出式的讲座，新一轮校园实践更强调参与感和节奏设计。\n\n" +
                        "课程往往由导入故事、动作模仿、角色体验和作品展示组成，学生更容易在短时间内建立兴趣。\n\n" +
                        "这也印证了平台在研学工坊和知识竞答模块上的价值，即把内容传播延伸为持续学习链路。",
                "教育观察",
                "校园实践",
                "success",
                "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=800&auto=format&fit=crop&q=60",
                "2026-02-20 13:20:00"
        );
        insertNewsArticle(
                "社区工坊成为城市更新中的非遗新场景",
                "越来越多街区改造项目开始把手作工坊、口述展示和主题路线纳入公共文化空间设计。",
                "在城市更新项目中，非遗工坊正在从“陈列内容”转向“日常空间服务”。\n\n" +
                        "居民可以通过预约体验、主题路线和社区故事分享，重新建立与地方文化的连接。\n\n" +
                        "这类资讯天然适合和地图、路线规划、项目详情形成联动，是平台值得继续深化的一条内容主线。",
                "城市文化观察",
                "城市更新",
                "warning",
                "https://images.unsplash.com/photo-1473448912268-2022ce9509d8?w=800&auto=format&fit=crop&q=60",
                "2026-02-15 18:40:00"
        );
    }

    private void seedQuizQuestions() {
        Long questionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quiz_question", Long.class);
        if (questionCount != null && questionCount > 0) {
            return;
        }

        insertQuizQuestion("被誉为“百戏之祖”的中国传统戏曲剧种是？", JSONUtil.toJsonStr(new String[]{"京剧", "昆曲", "越剧", "黄梅戏"}), 1, "昆曲形成较早，对中国戏曲体系影响深远，因此常被称为“百戏之祖”。", "戏曲", "medium", 1);
        insertQuizQuestion("下列哪一项不属于中国“四大名绣”？", JSONUtil.toJsonStr(new String[]{"苏绣", "湘绣", "蜀绣", "杭绣"}), 3, "四大名绣通常指苏绣、湘绣、粤绣和蜀绣，杭绣并不在其中。", "工艺", "easy", 2);
        insertQuizQuestion("二十四节气中，标志春季正式开始的节气是？", JSONUtil.toJsonStr(new String[]{"立春", "雨水", "惊蛰", "春分"}), 0, "立春意味着新的农事周期开始，是春季的起点。", "节俗", "easy", 3);
        insertQuizQuestion("中国古琴通常有几根弦？", JSONUtil.toJsonStr(new String[]{"五根", "六根", "七根", "八根"}), 2, "古琴也称七弦琴，常见制式为七根琴弦。", "器乐", "easy", 4);
        insertQuizQuestion("端午节赛龙舟、吃粽子的民间纪念对象通常是？", JSONUtil.toJsonStr(new String[]{"屈原", "伍子胥", "曹娥", "介子推"}), 0, "虽然端午习俗来源复杂，但最广为流传的纪念对象是屈原。", "节俗", "easy", 5);
        insertQuizQuestion("以下哪项更符合非遗“活态传承”的含义？", JSONUtil.toJsonStr(new String[]{"只保存历史照片", "让技艺在真实生活中持续使用", "只在节日集中展示", "只做线上资料存档"}), 1, "活态传承强调在现实生活中持续实践，而不是停留在静态展示。", "保护理念", "medium", 6);
        insertQuizQuestion("皮影戏表演中最核心的媒介组合通常是？", JSONUtil.toJsonStr(new String[]{"剪纸与木偶", "灯光、幕布与影人", "说书与鼓乐", "面具与服饰"}), 1, "皮影戏的核心在于灯光照射下，影人在幕布后形成动态影像。", "表演", "medium", 7);
        insertQuizQuestion("在非遗数字化场景中，下列哪项最能提升后续学习转化？", JSONUtil.toJsonStr(new String[]{"只展示静态海报", "阅读后推荐相关项目与研学内容", "隐藏项目信息", "取消标签和分类"}), 1, "把资讯阅读与项目、路线、课程继续串起来，才能形成真正有效的内容闭环。", "数字传播", "medium", 8);
    }

    private void insertNewsArticle(String title, String summary, String content, String source, String tag, String tagType, String coverImageUrl, String publishedAt) {
        jdbcTemplate.update(
                "INSERT INTO news_article (title, summary, content, source, tag, tag_type, cover_image_url, video_url, published_at, view_count, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                title, summary, content, source, tag, tagType, coverImageUrl, null, publishedAt
        );
    }

    private void ensureColumnExists(String tableName, String columnName, String ddl) {
        Boolean exists = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            if (hasColumn(metaData, connection.getCatalog(), tableName, columnName)) {
                return true;
            }
            return hasColumn(metaData, connection.getCatalog(), tableName.toUpperCase(), columnName.toUpperCase());
        });

        if (Boolean.FALSE.equals(exists)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private boolean hasColumn(DatabaseMetaData metaData, String catalog, String tableName, String columnName) throws SQLException {
        try (ResultSet resultSet = metaData.getColumns(catalog, null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private void insertQuizQuestion(String questionText, String optionsJson, Integer correctIndex, String explanation, String category, String difficulty, Integer sortOrder) {
        jdbcTemplate.update(
                "INSERT INTO quiz_question (question_text, options_json, correct_index, explanation, category, difficulty, sort_order, active, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                questionText, optionsJson, correctIndex, explanation, category, difficulty, sortOrder
        );
    }
}
