package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.common.enums.CatalogItemStatus;
import com.neusoft.cloudbrain.common.enums.ExamOrderType;
import com.neusoft.cloudbrain.dto.response.catalog.ExamItemVO;
import com.neusoft.cloudbrain.dto.response.catalog.LabItemVO;
import com.neusoft.cloudbrain.dto.response.medical.RecommendedExamVO;
import com.neusoft.cloudbrain.entity.Department;
import com.neusoft.cloudbrain.entity.ExamItem;
import com.neusoft.cloudbrain.entity.ExamOrder;
import com.neusoft.cloudbrain.entity.LabItem;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.ExamItemMapper;
import com.neusoft.cloudbrain.mapper.LabItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamCatalogService {

    private final LabItemMapper labItemMapper;
    private final ExamItemMapper examItemMapper;
    private final DepartmentMapper departmentMapper;

    @Transactional(readOnly = true)
    public List<LabItemVO> listEnabledLabItems(String keyword) {
        return loadEnabledLabItems().stream()
                .filter(item -> matchesKeyword(item, keyword))
                .map(this::toLabVO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamItemVO> listEnabledExamItems(String keyword) {
        return loadEnabledExamItems().stream()
                .filter(item -> matchesKeyword(item, keyword))
                .map(this::toExamVO)
                .toList();
    }

    @Transactional(readOnly = true)
    public String buildCatalogPromptText() {
        List<LabItem> labItems = loadEnabledLabItems();
        List<ExamItem> examItems = loadEnabledExamItems();
        if (labItems.isEmpty() && examItems.isEmpty()) {
            return "（当前无启用的检验/检查项目，recommendedExams 必须返回空数组 []）";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("只能从以下医院项目目录中选取，输出时 orderType、catalogItemId、itemCode、examType、bodyPart 必须与目录完全一致：\n");
        if (!labItems.isEmpty()) {
            sb.append("\n### 检验项目（orderType=LAB，无需设备）\n");
            sb.append("| catalogItemId | itemCode | itemName | category | fee |\n");
            sb.append("|---|---|---|---|---|\n");
            for (LabItem item : labItems) {
                sb.append("| ").append(item.getId())
                        .append(" | ").append(item.getItemCode())
                        .append(" | ").append(item.getItemName())
                        .append(" | ").append(nullToDash(item.getCategory()))
                        .append(" | ").append(item.getFee())
                        .append(" |\n");
            }
        }
        if (!examItems.isEmpty()) {
            sb.append("\n### 检查项目（orderType=EXAM，需设备）\n");
            sb.append("| catalogItemId | itemCode | itemName | bodyPart | equipmentType | fee |\n");
            sb.append("|---|---|---|---|---|---|\n");
            for (ExamItem item : examItems) {
                sb.append("| ").append(item.getId())
                        .append(" | ").append(item.getItemCode())
                        .append(" | ").append(item.getItemName())
                        .append(" | ").append(nullToDash(item.getBodyPart()))
                        .append(" | ").append(nullToDash(item.getEquipmentType()))
                        .append(" | ").append(item.getFee())
                        .append(" |\n");
            }
        }
        return sb.toString().trim();
    }

    @Transactional(readOnly = true)
    public List<RecommendedExamVO> recommendFromContext(String context, int maxItems) {
        String ctx = normalize(context);
        if (!StringUtils.hasText(ctx)) {
            return List.of();
        }
        List<LabItem> labItems = loadEnabledLabItems();
        List<ExamItem> examItems = loadEnabledExamItems();
        List<ScoredRecommendation> scored = new ArrayList<>();
        int priority = 1;
        for (LabItem item : labItems) {
            int score = scoreContextAgainstItem(ctx, item.getItemName(), item.getCategory(), item.getDescription());
            if (score > 0) {
                scored.add(new ScoredRecommendation(
                        toRecommendedExam(ExamOrderType.LAB, item.getId(), item.getItemCode(),
                                item.getItemName(), item.getCategory(), score, priority++),
                        score));
            }
        }
        for (ExamItem item : examItems) {
            int score = scoreContextAgainstItem(ctx, item.getItemName(), item.getBodyPart(),
                    item.getDescription(), item.getEquipmentType());
            if (score > 0) {
                scored.add(new ScoredRecommendation(
                        toRecommendedExam(ExamOrderType.EXAM, item.getId(), item.getItemCode(),
                                item.getItemName(), item.getBodyPart(), score, priority++),
                        score));
            }
        }
        if (scored.isEmpty()) {
            return List.of();
        }
        List<RecommendedExamVO> results = new ArrayList<>();
        int rank = 1;
        for (ScoredRecommendation entry : scored.stream()
                .sorted(Comparator.comparingInt(ScoredRecommendation::score).reversed())
                .limit(Math.max(1, maxItems))
                .toList()) {
            RecommendedExamVO exam = entry.exam();
            exam.setPriority(rank++);
            results.add(exam);
        }
        enrichRecommendedExams(results);
        return results;
    }

    @Transactional(readOnly = true)
    public void enrichRecommendedExams(List<RecommendedExamVO> exams) {
        if (exams == null || exams.isEmpty()) {
            return;
        }
        List<LabItem> labItems = loadEnabledLabItems();
        List<ExamItem> examItems = loadEnabledExamItems();
        for (RecommendedExamVO exam : exams) {
            if (resolveByCatalogId(exam, labItems, examItems)) {
                continue;
            }
            matchCatalogItem(exam, labItems, examItems).ifPresent(match -> applyMatch(exam, match));
            if (exam.getCatalogMatched() == null) {
                exam.setCatalogMatched(false);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<RecommendedExamVO> retainCatalogMatched(List<RecommendedExamVO> exams) {
        if (exams == null || exams.isEmpty()) {
            return List.of();
        }
        enrichRecommendedExams(exams);
        return exams.stream()
                .filter(exam -> Boolean.TRUE.equals(exam.getCatalogMatched()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LabItem getEnabledLabItem(Long id) {
        LabItem item = labItemMapper.selectById(id);
        if (item == null || item.getStatus() != CatalogItemStatus.ENABLED) {
            return null;
        }
        return item;
    }

    @Transactional(readOnly = true)
    public ExamItem getEnabledExamItem(Long id) {
        ExamItem item = examItemMapper.selectById(id);
        if (item == null || item.getStatus() != CatalogItemStatus.ENABLED) {
            return null;
        }
        return item;
    }

    /**
     * 解析检查/检验单应付金额：优先用开单时快照 fee，否则回查目录项单价。
     */
    @Transactional(readOnly = true)
    public java.math.BigDecimal resolveOrderFee(ExamOrder order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }
        if (order.getFee() != null) {
            return order.getFee();
        }
        if (order.getLabItemId() != null) {
            LabItem item = getEnabledLabItem(order.getLabItemId());
            if (item != null && item.getFee() != null) {
                return item.getFee();
            }
        }
        if (order.getExamItemId() != null) {
            ExamItem item = getEnabledExamItem(order.getExamItemId());
            if (item != null && item.getFee() != null) {
                return item.getFee();
            }
        }
        return BigDecimal.ZERO;
    }

    private List<LabItem> loadEnabledLabItems() {
        return labItemMapper.selectList(
                Wrappers.<LabItem>lambdaQuery()
                        .eq(LabItem::getStatus, CatalogItemStatus.ENABLED)
                        .orderByAsc(LabItem::getItemCode));
    }

    private List<ExamItem> loadEnabledExamItems() {
        return examItemMapper.selectList(
                Wrappers.<ExamItem>lambdaQuery()
                        .eq(ExamItem::getStatus, CatalogItemStatus.ENABLED)
                        .orderByAsc(ExamItem::getItemCode));
    }

    private boolean matchesKeyword(LabItem item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(item.getItemName(), kw)
                || containsIgnoreCase(item.getItemCode(), kw)
                || containsIgnoreCase(item.getCategory(), kw);
    }

    private boolean matchesKeyword(ExamItem item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        return containsIgnoreCase(item.getItemName(), kw)
                || containsIgnoreCase(item.getItemCode(), kw)
                || containsIgnoreCase(item.getBodyPart(), kw)
                || containsIgnoreCase(item.getEquipmentType(), kw);
    }

    private boolean resolveByCatalogId(RecommendedExamVO exam, List<LabItem> labItems, List<ExamItem> examItems) {
        if (exam.getCatalogItemId() == null) {
            return false;
        }
        if (exam.getOrderType() == ExamOrderType.LAB) {
            LabItem item = labItems.stream()
                    .filter(i -> i.getId().equals(exam.getCatalogItemId()))
                    .findFirst()
                    .orElse(null);
            if (item != null) {
                applyLabItem(exam, item);
                return true;
            }
        }
        if (exam.getOrderType() == ExamOrderType.EXAM) {
            ExamItem item = examItems.stream()
                    .filter(i -> i.getId().equals(exam.getCatalogItemId()))
                    .findFirst()
                    .orElse(null);
            if (item != null) {
                applyExamItem(exam, item);
                return true;
            }
        }
        if (exam.getOrderType() == null) {
            LabItem lab = labItems.stream()
                    .filter(i -> i.getId().equals(exam.getCatalogItemId()))
                    .findFirst()
                    .orElse(null);
            if (lab != null) {
                applyLabItem(exam, lab);
                return true;
            }
            ExamItem imaging = examItems.stream()
                    .filter(i -> i.getId().equals(exam.getCatalogItemId()))
                    .findFirst()
                    .orElse(null);
            if (imaging != null) {
                applyExamItem(exam, imaging);
                return true;
            }
        }
        return false;
    }

    private java.util.Optional<CatalogMatch> matchCatalogItem(RecommendedExamVO exam,
                                                              List<LabItem> labItems,
                                                              List<ExamItem> examItems) {
        if (!StringUtils.hasText(exam.getExamType())) {
            return java.util.Optional.empty();
        }
        List<CatalogMatch> candidates = new ArrayList<>();
        for (LabItem item : labItems) {
            int score = scoreMatch(exam.getExamType(), exam.getBodyPart(), item.getItemName(), item.getCategory());
            if (score > 0) {
                candidates.add(new CatalogMatch(ExamOrderType.LAB, item.getId(), item.getItemCode(),
                        item.getItemName(), item.getCategory(), score));
            }
        }
        for (ExamItem item : examItems) {
            int score = scoreMatch(exam.getExamType(), exam.getBodyPart(), item.getItemName(), item.getBodyPart());
            if (score > 0) {
                candidates.add(new CatalogMatch(ExamOrderType.EXAM, item.getId(), item.getItemCode(),
                        item.getItemName(), item.getBodyPart(), score));
            }
        }
        return candidates.stream().max(Comparator.comparingInt(CatalogMatch::score));
    }

    private void applyMatch(RecommendedExamVO exam, CatalogMatch match) {
        exam.setOrderType(match.orderType());
        exam.setCatalogItemId(match.itemId());
        exam.setItemCode(match.itemCode());
        exam.setCatalogMatched(true);
        exam.setExamType(match.itemName());
        if (StringUtils.hasText(match.displayPart())) {
            exam.setBodyPart(match.displayPart());
        }
    }

    private void applyLabItem(RecommendedExamVO exam, LabItem item) {
        exam.setOrderType(ExamOrderType.LAB);
        exam.setCatalogItemId(item.getId());
        exam.setItemCode(item.getItemCode());
        exam.setCatalogMatched(true);
        exam.setExamType(item.getItemName());
        exam.setBodyPart(item.getCategory());
    }

    private void applyExamItem(RecommendedExamVO exam, ExamItem item) {
        exam.setOrderType(ExamOrderType.EXAM);
        exam.setCatalogItemId(item.getId());
        exam.setItemCode(item.getItemCode());
        exam.setCatalogMatched(true);
        exam.setExamType(item.getItemName());
        exam.setBodyPart(item.getBodyPart());
    }

    private RecommendedExamVO toRecommendedExam(ExamOrderType orderType,
                                                Long itemId,
                                                String itemCode,
                                                String itemName,
                                                String bodyPart,
                                                int score,
                                                int priority) {
        RecommendedExamVO exam = RecommendedExamVO.builder()
                .orderType(orderType)
                .catalogItemId(itemId)
                .itemCode(itemCode)
                .examType(itemName)
                .bodyPart(bodyPart)
                .clinicalIndication("根据症状与目录项「" + itemName + "」的关联推荐")
                .priority(priority)
                .catalogMatched(true)
                .build();
        return exam;
    }

    private int scoreContextAgainstItem(String context, String... fields) {
        int score = 0;
        Set<String> tokens = new LinkedHashSet<>();
        for (String field : fields) {
            if (!StringUtils.hasText(field)) {
                continue;
            }
            String normalized = normalize(field);
            tokens.add(normalized);
            for (int len = Math.min(4, normalized.length()); len >= 2; len--) {
                for (int i = 0; i <= normalized.length() - len; i++) {
                    tokens.add(normalized.substring(i, i + len));
                }
            }
        }
        for (String token : tokens) {
            if (token.length() >= 2 && context.contains(token)) {
                score += token.length() >= 3 ? 15 : 8;
            }
        }
        return score;
    }

    private int scoreMatch(String aiName, String aiPart, String itemName, String itemPart) {
        String nameA = normalize(aiName);
        String nameB = normalize(itemName);
        if (!StringUtils.hasText(nameA) || !StringUtils.hasText(nameB)) {
            return 0;
        }
        int score = 0;
        if (nameA.equals(nameB)) {
            score += 100;
        } else if (nameB.contains(nameA) || nameA.contains(nameB)) {
            score += 60;
        } else {
            return 0;
        }
        if (StringUtils.hasText(aiPart) && StringUtils.hasText(itemPart)) {
            String partA = normalize(aiPart);
            String partB = normalize(itemPart);
            if (partA.equals(partB) || partB.contains(partA) || partA.contains(partB)) {
                score += 20;
            }
        }
        return score;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return StringUtils.hasText(text) && text.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private LabItemVO toLabVO(LabItem item) {
        return LabItemVO.builder()
                .id(item.getId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .category(item.getCategory())
                .description(item.getDescription())
                .departmentId(item.getDepartmentId())
                .departmentName(resolveDepartmentName(item.getDepartmentId()))
                .fee(item.getFee())
                .build();
    }

    private ExamItemVO toExamVO(ExamItem item) {
        return ExamItemVO.builder()
                .id(item.getId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .bodyPart(item.getBodyPart())
                .description(item.getDescription())
                .departmentId(item.getDepartmentId())
                .departmentName(resolveDepartmentName(item.getDepartmentId()))
                .equipmentType(item.getEquipmentType())
                .fee(item.getFee())
                .build();
    }

    private String resolveDepartmentName(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        Department department = departmentMapper.selectById(departmentId);
        return department != null ? department.getName() : null;
    }

    private record CatalogMatch(
            ExamOrderType orderType,
            Long itemId,
            String itemCode,
            String itemName,
            String displayPart,
            int score
    ) {
    }

    private record ScoredRecommendation(RecommendedExamVO exam, int score) {
    }
}
