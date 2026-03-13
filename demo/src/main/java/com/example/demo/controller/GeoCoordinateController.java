package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.IchProject;
import com.example.demo.entity.IchRegion;
import com.example.demo.mapper.IchProjectMapper;
import com.example.demo.mapper.IchRegionMapper;
import com.example.demo.request.GeoCoordinateUpdateRequest;
import com.example.demo.service.IchProjectService;
import com.example.demo.util.RequestAuthUtil;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/geo")
public class GeoCoordinateController {

    private final IchProjectService projectService;
    private final IchProjectMapper projectMapper;
    private final IchRegionMapper regionMapper;
    private final RequestAuthUtil requestAuthUtil;

    public GeoCoordinateController(IchProjectService projectService,
                                   IchProjectMapper projectMapper,
                                   IchRegionMapper regionMapper,
                                   RequestAuthUtil requestAuthUtil) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
        this.regionMapper = regionMapper;
        this.requestAuthUtil = requestAuthUtil;
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("仅管理员可查看地理数据概览");
        }

        List<IchProject> projects = projectService.list(new QueryWrapper<IchProject>().select("id", "region_id", "longitude", "latitude"));
        long totalProjects = projects.size();
        long directProjectsWithCoordinates = projects.stream()
                .filter(project -> project.getLongitude() != null && project.getLatitude() != null)
                .count();
        List<Long> regionIds = projects.stream()
                .map(IchProject::getRegionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, IchRegion> regionMap = regionIds.isEmpty()
                ? new LinkedHashMap<>()
                : regionMapper.selectBatchIds(regionIds).stream()
                .collect(Collectors.toMap(IchRegion::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        long effectiveProjectsWithCoordinates = projects.stream()
                .filter(project -> hasEffectiveCoordinates(project, regionMap))
                .count();
        long totalRegions = regionMapper.selectCount(null);
        long regionsWithCoordinates = regionMapper.selectCount(hasCoordinatesRegionWrapper());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalProjects", totalProjects);
        summary.put("projectsWithCoordinates", effectiveProjectsWithCoordinates);
        summary.put("projectsMissingCoordinates", Math.max(totalProjects - effectiveProjectsWithCoordinates, 0));
        summary.put("directProjectsWithCoordinates", directProjectsWithCoordinates);
        summary.put("directProjectsMissingCoordinates", Math.max(totalProjects - directProjectsWithCoordinates, 0));
        summary.put("totalRegions", totalRegions);
        summary.put("regionsWithCoordinates", regionsWithCoordinates);
        summary.put("regionsMissingCoordinates", Math.max(totalRegions - regionsWithCoordinates, 0));
        return Result.success(summary);
    }

    @GetMapping("/projects/missing")
    public Result<Page<IchProject>> listProjectsMissingCoordinates(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                   @RequestParam(defaultValue = "20") Integer pageSize,
                                                                   @RequestParam(required = false) String keyword,
                                                                   HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("仅管理员可查看缺失坐标项目");
        }

        QueryWrapper<IchProject> query = buildMissingProjectQuery(keyword);
        Page<IchProject> page = projectService.pageWithViewCount(new Page<>(pageNum, pageSize), query);
        projectService.populateProjectRelations(page.getRecords());
        return Result.success(page);
    }

    @GetMapping("/regions/missing")
    public Result<List<IchRegion>> listRegionsMissingCoordinates(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) Integer level,
                                                                 HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("仅管理员可查看缺失坐标地区");
        }

        LambdaQueryWrapper<IchRegion> query = missingCoordinatesRegionWrapper();
        if (StringUtils.hasText(keyword)) {
            query.like(IchRegion::getName, keyword.trim());
        }
        if (level != null) {
            query.eq(IchRegion::getLevel, level);
        }
        query.orderByAsc(IchRegion::getLevel).orderByAsc(IchRegion::getId);
        return Result.success(regionMapper.selectList(query));
    }

    @PutMapping("/projects/{id}")
    public Result<Boolean> updateProjectCoordinates(@PathVariable Long id,
                                                    @RequestBody GeoCoordinateUpdateRequest body,
                                                    HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("仅管理员可维护项目坐标");
        }

        IchProject project = projectService.getById(id);
        if (project == null) {
            return Result.error("项目不存在");
        }

        String validationError = validateCoordinatePayload(body, true);
        if (validationError != null) {
            return Result.error(validationError);
        }

        LambdaUpdateWrapper<IchProject> update = new LambdaUpdateWrapper<IchProject>()
                .eq(IchProject::getId, id)
                .set(IchProject::getLongitude, body.getLongitude())
                .set(IchProject::getLatitude, body.getLatitude());
        if (body.getAddress() != null) {
            update.set(IchProject::getAddress, body.getAddress().trim());
        }
        if (body.getContactPhone() != null) {
            update.set(IchProject::getContactPhone, body.getContactPhone().trim());
        }
        if (body.getOpeningHours() != null) {
            update.set(IchProject::getOpeningHours, body.getOpeningHours().trim());
        }
        return Result.success(projectService.update(update));
    }

    @PutMapping("/regions/{id}")
    public Result<Boolean> updateRegionCoordinates(@PathVariable Long id,
                                                   @RequestBody GeoCoordinateUpdateRequest body,
                                                   HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("仅管理员可维护地区坐标");
        }

        IchRegion region = regionMapper.selectById(id);
        if (region == null) {
            return Result.error("地区不存在");
        }

        String validationError = validateCoordinatePayload(body, false);
        if (validationError != null) {
            return Result.error(validationError);
        }

        LambdaUpdateWrapper<IchRegion> update = new LambdaUpdateWrapper<IchRegion>()
                .eq(IchRegion::getId, id)
                .set(IchRegion::getLongitude, body.getLongitude())
                .set(IchRegion::getLatitude, body.getLatitude());
        return Result.success(regionMapper.update(null, update) > 0);
    }

    @PostMapping("/projects/import-csv")
    public Result<Map<String, Object>> importProjectCoordinates(@RequestParam("file") MultipartFile file,
                                                                HttpServletRequest request) throws IOException {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("仅管理员可批量导入项目坐标");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("请上传 CSV 文件");
        }

        List<Map<String, String>> rows = parseCsv(file);
        int updatedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        for (int index = 0; index < rows.size(); index += 1) {
            Map<String, String> row = rows.get(index);
            String rowLabel = "第 " + (index + 2) + " 行";
            IchProject project = resolveProject(row);
            if (project == null) {
                skippedCount += 1;
                errors.add(rowLabel + " 未找到匹配项目，请填写 id 或准确项目名称");
                continue;
            }

            BigDecimal longitude = parseDecimalField(row, "longitude", "经度");
            BigDecimal latitude = parseDecimalField(row, "latitude", "纬度");
            if (longitude == null || latitude == null) {
                skippedCount += 1;
                errors.add(rowLabel + " 缺少有效经纬度");
                continue;
            }
            if (!isValidLongitude(longitude) || !isValidLatitude(latitude)) {
                skippedCount += 1;
                errors.add(rowLabel + " 经纬度超出有效范围");
                continue;
            }

            LambdaUpdateWrapper<IchProject> update = new LambdaUpdateWrapper<IchProject>()
                    .eq(IchProject::getId, project.getId())
                    .set(IchProject::getLongitude, longitude)
                    .set(IchProject::getLatitude, latitude);

            String address = firstValue(row, "address", "地址");
            String contactPhone = firstValue(row, "contactPhone", "联系电话", "contact_phone");
            String openingHours = firstValue(row, "openingHours", "开放时间", "opening_hours");
            if (address != null) {
                update.set(IchProject::getAddress, address);
            }
            if (contactPhone != null) {
                update.set(IchProject::getContactPhone, contactPhone);
            }
            if (openingHours != null) {
                update.set(IchProject::getOpeningHours, openingHours);
            }

            if (projectService.update(update)) {
                updatedCount += 1;
            } else {
                skippedCount += 1;
                errors.add(rowLabel + " 更新失败");
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("skippedCount", skippedCount);
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        return Result.success(result);
    }

    @PostMapping("/regions/import-csv")
    public Result<Map<String, Object>> importRegionCoordinates(@RequestParam("file") MultipartFile file,
                                                               HttpServletRequest request) throws IOException {
        if (!requestAuthUtil.isAdmin(request)) {
            return Result.error("仅管理员可批量导入地区坐标");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("请上传 CSV 文件");
        }

        List<Map<String, String>> rows = parseCsv(file);
        int updatedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        for (int index = 0; index < rows.size(); index += 1) {
            Map<String, String> row = rows.get(index);
            String rowLabel = "第 " + (index + 2) + " 行";
            IchRegion region = resolveRegion(row);
            if (region == null) {
                skippedCount += 1;
                errors.add(rowLabel + " 未找到匹配地区，请填写 id 或准确地区名称");
                continue;
            }

            BigDecimal longitude = parseDecimalField(row, "longitude", "经度");
            BigDecimal latitude = parseDecimalField(row, "latitude", "纬度");
            if (longitude == null || latitude == null) {
                skippedCount += 1;
                errors.add(rowLabel + " 缺少有效经纬度");
                continue;
            }
            if (!isValidLongitude(longitude) || !isValidLatitude(latitude)) {
                skippedCount += 1;
                errors.add(rowLabel + " 经纬度超出有效范围");
                continue;
            }

            LambdaUpdateWrapper<IchRegion> update = new LambdaUpdateWrapper<IchRegion>()
                    .eq(IchRegion::getId, region.getId())
                    .set(IchRegion::getLongitude, longitude)
                    .set(IchRegion::getLatitude, latitude);

            if (regionMapper.update(null, update) > 0) {
                updatedCount += 1;
            } else {
                skippedCount += 1;
                errors.add(rowLabel + " 更新失败");
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("skippedCount", skippedCount);
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        return Result.success(result);
    }

    @GetMapping("/projects/import-template")
    public ResponseEntity<byte[]> exportProjectTemplate(HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return forbiddenCsv("仅管理员可下载项目坐标模板");
        }
        String csv = String.join("\r\n",
                "id,name,longitude,latitude,address,contactPhone,openingHours",
                "1,项目名称,119.296494,26.074508,示例地址,0591-12345678,09:00-17:00"
        );
        return csvResponse("project-coordinate-template.csv", csv);
    }

    @GetMapping("/regions/import-template")
    public ResponseEntity<byte[]> exportRegionTemplate(HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return forbiddenCsv("仅管理员可下载地区坐标模板");
        }
        String csv = String.join("\r\n",
                "id,name,longitude,latitude",
                "1,福建省,119.296494,26.074508"
        );
        return csvResponse("region-coordinate-template.csv", csv);
    }

    @GetMapping("/projects/missing/export")
    public ResponseEntity<byte[]> exportProjectsMissingCoordinates(@RequestParam(required = false) String keyword,
                                                                   HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return forbiddenCsv("仅管理员可导出缺失坐标项目");
        }

        QueryWrapper<IchProject> query = buildMissingProjectQuery(keyword);
        List<IchProject> projects = projectService.list(query);
        projectService.populateProjectRelations(projects);
        StringBuilder builder = new StringBuilder("id,name,regionName,longitude,latitude,address,contactPhone,openingHours\r\n");
        for (IchProject project : projects) {
            builder.append(csvValue(project.getId()))
                    .append(',').append(csvValue(project.getName()))
                    .append(',').append(csvValue(project.getRegionName()))
                    .append(',').append(csvValue(project.getLongitude()))
                    .append(',').append(csvValue(project.getLatitude()))
                    .append(',').append(csvValue(project.getAddress()))
                    .append(',').append(csvValue(project.getContactPhone()))
                    .append(',').append(csvValue(project.getOpeningHours()))
                    .append("\r\n");
        }
        return csvResponse("project-missing-coordinates-" + LocalDate.now() + ".csv", builder.toString());
    }

    @GetMapping("/regions/missing/export")
    public ResponseEntity<byte[]> exportRegionsMissingCoordinates(@RequestParam(required = false) String keyword,
                                                                  @RequestParam(required = false) Integer level,
                                                                  HttpServletRequest request) {
        if (!requestAuthUtil.isAdmin(request)) {
            return forbiddenCsv("仅管理员可导出缺失坐标地区");
        }

        LambdaQueryWrapper<IchRegion> query = missingCoordinatesRegionWrapper();
        if (StringUtils.hasText(keyword)) {
            query.like(IchRegion::getName, keyword.trim());
        }
        if (level != null) {
            query.eq(IchRegion::getLevel, level);
        }
        query.orderByAsc(IchRegion::getLevel).orderByAsc(IchRegion::getId);
        List<IchRegion> regions = regionMapper.selectList(query);
        StringBuilder builder = new StringBuilder("id,name,level,parentId,longitude,latitude\r\n");
        for (IchRegion region : regions) {
            builder.append(csvValue(region.getId()))
                    .append(',').append(csvValue(region.getName()))
                    .append(',').append(csvValue(region.getLevel()))
                    .append(',').append(csvValue(region.getParentId()))
                    .append(',').append(csvValue(region.getLongitude()))
                    .append(',').append(csvValue(region.getLatitude()))
                    .append("\r\n");
        }
        return csvResponse("region-missing-coordinates-" + LocalDate.now() + ".csv", builder.toString());
    }

    private QueryWrapper<IchProject> buildMissingProjectQuery(String keyword) {
        QueryWrapper<IchProject> query = new QueryWrapper<>();
        query.and(wrapper -> wrapper.isNull("longitude").or().isNull("latitude"));
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper.like("name", value).or().like("address", value));
        }
        query.orderByAsc("audit_status").orderByDesc("id");
        return query;
    }

    private LambdaQueryWrapper<IchProject> hasCoordinatesProjectWrapper() {
        return new LambdaQueryWrapper<IchProject>()
                .isNotNull(IchProject::getLongitude)
                .isNotNull(IchProject::getLatitude);
    }

    private boolean hasEffectiveCoordinates(IchProject project, Map<Long, IchRegion> regionMap) {
        if (project == null) {
            return false;
        }
        if (project.getLongitude() != null && project.getLatitude() != null) {
            return true;
        }
        IchRegion region = regionMap.get(project.getRegionId());
        return region != null && region.getLongitude() != null && region.getLatitude() != null;
    }

    private LambdaQueryWrapper<IchRegion> hasCoordinatesRegionWrapper() {
        return new LambdaQueryWrapper<IchRegion>()
                .isNotNull(IchRegion::getLongitude)
                .isNotNull(IchRegion::getLatitude);
    }

    private LambdaQueryWrapper<IchRegion> missingCoordinatesRegionWrapper() {
        return new LambdaQueryWrapper<IchRegion>()
                .and(wrapper -> wrapper.isNull(IchRegion::getLongitude).or().isNull(IchRegion::getLatitude));
    }

    private String validateCoordinatePayload(GeoCoordinateUpdateRequest body, boolean allowProjectFields) {
        if (body == null) {
            return "请求体不能为空";
        }
        if (body.getLongitude() == null || body.getLatitude() == null) {
            return "经纬度不能为空";
        }
        if (!isValidLongitude(body.getLongitude()) || !isValidLatitude(body.getLatitude())) {
            return "经纬度超出有效范围";
        }
        if (!allowProjectFields && (body.getAddress() != null || body.getContactPhone() != null || body.getOpeningHours() != null)) {
            return "地区坐标接口不支持地址和开放信息字段";
        }
        return null;
    }

    private boolean isValidLongitude(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.valueOf(-180)) >= 0 && value.compareTo(BigDecimal.valueOf(180)) <= 0;
    }

    private boolean isValidLatitude(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.valueOf(-90)) >= 0 && value.compareTo(BigDecimal.valueOf(90)) <= 0;
    }

    private ResponseEntity<byte[]> csvResponse(String filename, String content) {
        byte[] bytes = ("\uFEFF" + content).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private ResponseEntity<byte[]> forbiddenCsv(String message) {
        return ResponseEntity.status(403)
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(message.getBytes(StandardCharsets.UTF_8));
    }

    private List<Map<String, String>> parseCsv(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "");
        List<String> lines = splitCsvLines(content);
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> headers = parseCsvRow(lines.get(0)).stream()
                .map(this::normalizeHeader)
                .collect(Collectors.toList());

        List<Map<String, String>> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index += 1) {
            String line = lines.get(index);
            if (!StringUtils.hasText(line)) {
                continue;
            }

            List<String> values = parseCsvRow(line);
            Map<String, String> row = new LinkedHashMap<>();
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex += 1) {
                String header = headers.get(columnIndex);
                String value = columnIndex < values.size() ? values.get(columnIndex).trim() : "";
                row.put(header, value);
            }
            rows.add(row);
        }
        return rows;
    }

    private List<String> splitCsvLines(String content) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < content.length(); index += 1) {
            char currentChar = content.charAt(index);
            if (currentChar == '"') {
                if (inQuotes && index + 1 < content.length() && content.charAt(index + 1) == '"') {
                    current.append('"');
                    index += 1;
                    continue;
                }
                inQuotes = !inQuotes;
                current.append(currentChar);
                continue;
            }
            if (!inQuotes && (currentChar == '\n' || currentChar == '\r')) {
                if (currentChar == '\r' && index + 1 < content.length() && content.charAt(index + 1) == '\n') {
                    index += 1;
                }
                lines.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(currentChar);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private List<String> parseCsvRow(String row) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < row.length(); index += 1) {
            char currentChar = row.charAt(index);
            if (currentChar == '"') {
                if (inQuotes && index + 1 < row.length() && row.charAt(index + 1) == '"') {
                    current.append('"');
                    index += 1;
                    continue;
                }
                inQuotes = !inQuotes;
                continue;
            }
            if (!inQuotes && currentChar == ',') {
                columns.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(currentChar);
        }
        columns.add(current.toString());
        return columns;
    }

    private String normalizeHeader(String header) {
        return header == null ? "" : header.replace("\uFEFF", "").trim();
    }

    private IchProject resolveProject(Map<String, String> row) {
        Long id = parseLong(firstValue(row, "id", "projectId", "项目ID"));
        if (id != null) {
            return projectMapper.selectById(id);
        }
        String name = firstValue(row, "name", "projectName", "项目名称");
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return projectMapper.selectOne(new LambdaQueryWrapper<IchProject>().eq(IchProject::getName, name.trim()).last("LIMIT 1"));
    }

    private IchRegion resolveRegion(Map<String, String> row) {
        Long id = parseLong(firstValue(row, "id", "regionId", "地区ID"));
        if (id != null) {
            return regionMapper.selectById(id);
        }
        String name = firstValue(row, "name", "regionName", "地区名称");
        if (!StringUtils.hasText(name)) {
            return null;
        }
        List<IchRegion> matched = regionMapper.selectList(new LambdaQueryWrapper<IchRegion>()
                .eq(IchRegion::getName, name.trim())
                .last("LIMIT 1"));
        return matched.isEmpty() ? null : matched.get(0);
    }

    private String firstValue(Map<String, String> row, String... keys) {
        return Arrays.stream(keys)
                .map(row::get)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal parseDecimalField(Map<String, String> row, String... keys) {
        String value = firstValue(row, keys);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = Objects.toString(value, "");
        String escaped = text.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }
}
