package com.neusoft.cloudbrain.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.neusoft.cloudbrain.dto.request.prescription.PrescriptionItemRequest;
import com.neusoft.cloudbrain.entity.Drug;
import com.neusoft.cloudbrain.mapper.DrugMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DrugCatalogService {

    private final DrugMapper drugMapper;

    @Transactional(readOnly = true)
    public String buildCatalogPromptText() {
        List<Drug> drugs = loadActiveDrugs();
        if (drugs.isEmpty()) {
            return "（当前无在售药品，recommendedItems 必须返回空数组 []）";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("只能从以下药品目录中选取，输出时 drugId、drugCode、drugName 必须与目录完全一致：\n\n");
        sb.append("| drugId | drugCode | drugName | specification | unit | unitPrice | stock | contraindication |\n");
        sb.append("|---|---|---|---|---|---|---|---|\n");
        for (Drug drug : drugs) {
            sb.append("| ").append(drug.getId())
                    .append(" | ").append(drug.getDrugCode())
                    .append(" | ").append(drug.getDrugName())
                    .append(" | ").append(nullToDash(drug.getSpecification()))
                    .append(" | ").append(nullToDash(drug.getUnit()))
                    .append(" | ").append(drug.getUnitPrice() != null ? drug.getUnitPrice() : "-")
                    .append(" | ").append(drug.getStockQuantity() != null ? drug.getStockQuantity() : "-")
                    .append(" | ").append(nullToDash(drug.getContraindication()))
                    .append(" |\n");
        }
        return sb.toString().trim();
    }

    @Transactional(readOnly = true)
    public Drug getActiveDrug(Long drugId) {
        if (drugId == null) {
            return null;
        }
        Drug drug = drugMapper.selectById(drugId);
        if (drug == null || drug.getStatus() == null || drug.getStatus() != 1) {
            return null;
        }
        return drug;
    }

    @Transactional(readOnly = true)
    public Drug getActiveDrugByCode(String drugCode) {
        if (!StringUtils.hasText(drugCode)) {
            return null;
        }
        Drug drug = drugMapper.selectOne(
                Wrappers.<Drug>lambdaQuery().eq(Drug::getDrugCode, drugCode.trim())
        );
        if (drug == null || drug.getStatus() == null || drug.getStatus() != 1) {
            return null;
        }
        return drug;
    }

    @Transactional(readOnly = true)
    public void enrichRecommendedItems(List<PrescriptionItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Drug> drugs = loadActiveDrugs();
        for (PrescriptionItemRequest item : items) {
            if (resolveByDrugId(item, drugs)) {
                continue;
            }
            matchDrugItem(item, drugs).ifPresent(drug -> applyDrug(item, drug));
            if (item.getCatalogMatched() == null) {
                item.setCatalogMatched(false);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PrescriptionItemRequest> retainCatalogMatched(List<PrescriptionItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        enrichRecommendedItems(items);
        return items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCatalogMatched()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionItemRequest> recommendFromContext(String context, String allergyHistory, int maxItems) {
        String ctx = normalize(context);
        if (!StringUtils.hasText(ctx)) {
            return List.of();
        }
        List<Drug> drugs = loadActiveDrugs();
        List<ScoredDrug> scored = new ArrayList<>();
        for (Drug drug : drugs) {
            if (isAllergyConflict(drug, allergyHistory)) {
                continue;
            }
            int score = scoreContextAgainstDrug(ctx, drug);
            if (score > 0) {
                scored.add(new ScoredDrug(drug, score));
            }
        }
        if (scored.isEmpty()) {
            return List.of();
        }
        List<PrescriptionItemRequest> results = new ArrayList<>();
        for (ScoredDrug entry : scored.stream()
                .sorted(Comparator.comparingInt(ScoredDrug::score).reversed())
                .limit(Math.max(1, maxItems))
                .toList()) {
            Drug drug = entry.drug();
            PrescriptionItemRequest item = new PrescriptionItemRequest();
            applyDrug(item, drug);
            item.setDosage(java.math.BigDecimal.ONE);
            item.setUnit(StringUtils.hasText(drug.getUnit()) ? drug.getUnit() : "片");
            item.setFrequency("每日1次");
            item.setDurationDays(7);
            results.add(item);
        }
        return results;
    }

    @Transactional(readOnly = true)
    public Drug resolveForPrescription(PrescriptionItemRequest itemReq) {
        if (itemReq.getDrugId() != null) {
            return getActiveDrug(itemReq.getDrugId());
        }
        if (StringUtils.hasText(itemReq.getDrugCode())) {
            Drug drug = getActiveDrugByCode(itemReq.getDrugCode());
            if (drug != null) {
                return drug;
            }
        }
        if (StringUtils.hasText(itemReq.getDrugName())) {
            PrescriptionItemRequest probe = new PrescriptionItemRequest();
            probe.setDrugName(itemReq.getDrugName());
            probe.setDrugCode(itemReq.getDrugCode());
            return matchDrugItem(probe, loadActiveDrugs()).orElse(null);
        }
        return null;
    }

    private List<Drug> loadActiveDrugs() {
        return drugMapper.selectList(
                Wrappers.<Drug>lambdaQuery()
                        .eq(Drug::getStatus, 1)
                        .orderByAsc(Drug::getDrugCode));
    }

    private boolean resolveByDrugId(PrescriptionItemRequest item, List<Drug> drugs) {
        if (item.getDrugId() == null) {
            return false;
        }
        Drug drug = drugs.stream().filter(d -> d.getId().equals(item.getDrugId())).findFirst().orElse(null);
        if (drug != null) {
            applyDrug(item, drug);
            return true;
        }
        return false;
    }

    private java.util.Optional<Drug> matchDrugItem(PrescriptionItemRequest item, List<Drug> drugs) {
        if (!StringUtils.hasText(item.getDrugName()) && !StringUtils.hasText(item.getDrugCode())) {
            return java.util.Optional.empty();
        }
        List<ScoredDrug> candidates = new ArrayList<>();
        for (Drug drug : drugs) {
            int score = scoreDrugMatch(item.getDrugName(), item.getDrugCode(), drug);
            if (score > 0) {
                candidates.add(new ScoredDrug(drug, score));
            }
        }
        return candidates.stream()
                .max(Comparator.comparingInt(ScoredDrug::score))
                .map(ScoredDrug::drug);
    }

    private void applyDrug(PrescriptionItemRequest item, Drug drug) {
        item.setDrugId(drug.getId());
        item.setDrugCode(drug.getDrugCode());
        item.setDrugName(drug.getDrugName());
        if (!StringUtils.hasText(item.getUnit()) && StringUtils.hasText(drug.getUnit())) {
            item.setUnit(drug.getUnit());
        }
        item.setCatalogMatched(true);
    }

    private int scoreDrugMatch(String drugName, String drugCode, Drug drug) {
        int score = 0;
        if (StringUtils.hasText(drugCode) && drugCode.equalsIgnoreCase(drug.getDrugCode())) {
            score += 120;
        }
        if (StringUtils.hasText(drugName)) {
            String nameA = normalize(drugName);
            String nameB = normalize(drug.getDrugName());
            if (nameA.equals(nameB)) {
                score += 100;
            } else if (nameB.contains(nameA) || nameA.contains(nameB)) {
                score += 60;
            } else {
                return score > 0 ? score : 0;
            }
        }
        return score;
    }

    private int scoreContextAgainstDrug(String context, Drug drug) {
        return scoreContextAgainstFields(context,
                drug.getDrugName(), drug.getSpecification(), drug.getContraindication());
    }

    private int scoreContextAgainstFields(String context, String... fields) {
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

    private boolean isAllergyConflict(Drug drug, String allergyHistory) {
        if (!StringUtils.hasText(allergyHistory) || "无".equals(allergyHistory.trim())) {
            return false;
        }
        String allergy = normalize(allergyHistory);
        if (StringUtils.hasText(drug.getContraindication()) && normalize(drug.getContraindication()).contains(allergy)) {
            return true;
        }
        return StringUtils.hasText(drug.getDrugName()) && allergy.contains("青霉素")
                && normalize(drug.getDrugName()).contains("青霉素");
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private record ScoredDrug(Drug drug, int score) {
    }
}
