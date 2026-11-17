package com.neusoft.cloudbrain.service.ai;

import com.neusoft.cloudbrain.dto.response.ai.ExamReportDraftVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.function.Supplier;

@Slf4j
@Service
public class AiExamReportService {

    private final java.util.Random random = new java.util.Random();

    public ExamReportDraftVO generateExamReport(String examType, String bodyPart, String indication) {
        String type = defaultText(examType, "常规检查");
        String part = StringUtils.hasText(bodyPart) ? bodyPart.trim() : "";
        String key = (type + " " + part).toLowerCase(Locale.ROOT);

        ReportContent content = resolveContent(key, type, part, indication);
        log.info("Simulated exam report generated type={} part={}", type, part);
        return ExamReportDraftVO.builder()
                .reportText(content.reportText())
                .conclusion(content.conclusion())
                .aiAvailable(true)
                .build();
    }

    private ReportContent resolveContent(String key, String type, String part, String indication) {
        if (matches(key, "心电图", "ecg", "动态心电")) {
            return generateEcg(part);
        }
        if (matches(key, "心肌酶", "肌钙蛋白", "troponin", "ck-mb")) {
            return generateCardiacEnzyme();
        }
        if (matches(key, "血常规", "cbc", "全血细胞")) {
            return generateBloodRoutine();
        }
        if (matches(key, "尿常规", "尿液分析")) {
            return generateUrinalysis();
        }
        if (matches(key, "肝功能", "alt", "ast", "转氨酶")) {
            return generateLiverFunction();
        }
        if (matches(key, "肾功能", "肌酐", "尿素", "egfr")) {
            return generateRenalFunction();
        }
        if (matches(key, "血糖", "葡萄糖", "糖化", "hba1c")) {
            return generateGlucose();
        }
        if (matches(key, "血脂", "胆固醇", "甘油三酯", "ldl", "hdl")) {
            return generateLipidProfile();
        }
        if (matches(key, "凝血", "pt", "aptt", "d-二聚体", "d二聚体")) {
            return generateCoagulation(key);
        }
        if (matches(key, "甲状腺", "tsh", "t3", "t4")) {
            return generateThyroid();
        }
        if (matches(key, "肿瘤标志物", "cea", "afp", "ca125", "ca199", "psa")) {
            return generateTumorMarker();
        }
        if (matches(key, "电解质", "钾", "钠", "氯")) {
            return generateElectrolyte();
        }
        if (matches(key, "crp", "c反应蛋白", "降钙素原", "pct")) {
            return generateInflammationMarker(key);
        }
        if (matches(key, "乙肝", "肝炎标志物", "hbv", "丙肝", "hcv")) {
            return generateHepatitisMarker();
        }
        if (matches(key, "胸片", "x线", "x光", "dr胸")) {
            return generateChestXray(part);
        }
        if (matches(key, "ct") && matches(key, "头", "颅", "脑")) {
            return generateHeadCt(part);
        }
        if (matches(key, "ct") && matches(key, "胸", "肺")) {
            return generateChestCt(part);
        }
        if (matches(key, "ct") && matches(key, "腹", "肝", "肾")) {
            return generateAbdominalCt(part);
        }
        if (matches(key, "ct")) {
            return generateGenericCt(type, part);
        }
        if (matches(key, "mri", "磁共振", "核磁")) {
            return generateMri(type, part);
        }
        if (matches(key, "超声", "b超", "彩超")) {
            return generateUltrasound(type, part);
        }
        if (matches(key, "心脏超声", "echocardiography", "心超")) {
            return generateEchocardiography();
        }
        if (matches(key, "胃镜", "上消化道内镜")) {
            return generateGastroscopy();
        }
        if (matches(key, "肠镜", "结肠镜")) {
            return generateColonoscopy();
        }
        if (matches(key, "肺功能", "spirometry", "通气功能")) {
            return generatePulmonaryFunction();
        }
        if (matches(key, "骨密度", "dexa")) {
            return generateBoneDensity(part);
        }
        if (matches(key, "病理", "活检")) {
            return generatePathology(type, part);
        }
        if (matches(key, "血压", "动态血压", "abpm")) {
            return generateBloodPressure();
        }
        if (matches(key, "眼压", "视野", "oct")) {
            return generateOphthalmology(key);
        }
        if (matches(key, "听力", "纯音", "audiometry")) {
            return generateAudiometry();
        }
        if (matches(key, "血气", "abg")) {
            return generateBloodGas();
        }
        return generateDefault(type, part, indication);
    }

    // ---------- 检验类 ----------

    private ReportContent generateEcg(String part) {
        int hr = randomInt(60, 95);
        boolean abnormal = isAbnormal();
        if (abnormal) {
            int abnormalHr = random.nextBoolean() ? randomInt(101, 118) : randomInt(48, 58);
            return new ReportContent(
                    String.format("检查部位：%s。窦性心律，心率 %d 次/分，ST 段压低 0.05~0.1mV，T 波低平（参考：心率 60~100 次/分）。",
                            partLabel(part, "心脏"), abnormalHr),
                    "心电图提示心肌供血可能不足，建议结合临床症状、心肌酶及动态观察。");
        }
        return new ReportContent(
                String.format("检查部位：%s。窦性心律，心率 %d 次/分，P-R 间期 0.12~0.20s，QRS 时限 <0.12s，ST-T 未见明显异常（参考：心率 60~100 次/分）。",
                        partLabel(part, "心脏"), hr),
                "心电图未见明显异常。");
    }

    private ReportContent generateCardiacEnzyme() {
        boolean abnormal = isAbnormal();
        if (abnormal) {
            double troponin = round(0.05 + random.nextDouble() * 0.25, 3);
            return new ReportContent(
                    String.format("肌钙蛋白 I %.3f ng/mL（参考 <0.04）；CK-MB %.1f U/L（参考 0~25）；CK %d U/L（参考 38~174）。",
                            troponin, round(28 + random.nextDouble() * 40, 1), randomInt(120, 280)),
                    "心肌酶谱升高，提示心肌损伤可能，建议结合心电图及临床进一步评估。");
        }
        return new ReportContent(
                String.format("肌钙蛋白 I %.3f ng/mL（参考 <0.04）；CK-MB %.1f U/L（参考 0~25）；CK %d U/L（参考 38~174）。",
                        round(0.005 + random.nextDouble() * 0.025, 3),
                        round(8 + random.nextDouble() * 12, 1),
                        randomInt(60, 140)),
                "心肌酶谱未见明显升高，不支持急性心肌梗死。");
    }

    private ReportContent generateBloodRoutine() {
        boolean abnormal = isAbnormal();
        double wbc = abnormal ? round(11.5 + random.nextDouble() * 6, 1) : round(4.5 + random.nextDouble() * 5, 1);
        double hb = abnormal ? round(95 + random.nextDouble() * 15, 0) : round(125 + random.nextDouble() * 25, 0);
        double plt = round(150 + random.nextDouble() * 150, 0);
        String report = String.format(
                "白细胞 %.1f×10^9/L（参考 3.5~9.5）；红细胞 %.2f×10^12/L；血红蛋白 %.0f g/L（参考 115~150）；血小板 %.0f×10^9/L（参考 125~350）；中性粒细胞比例 %.1f%%。",
                wbc, round(4.0 + random.nextDouble() * 1.2, 2), hb, plt, round(55 + random.nextDouble() * 20, 1));
        if (abnormal) {
            return new ReportContent(report, wbc > 9.5 ? "血常规提示感染可能，建议结合临床。" : "血常规提示轻度贫血，建议进一步查明原因。");
        }
        return new ReportContent(report, "血常规未见明显异常。");
    }

    private ReportContent generateUrinalysis() {
        boolean abnormal = isAbnormal();
        if (abnormal) {
            return new ReportContent(
                    "尿比重 1.020；pH 6.0；蛋白 (+)；隐血 (+)；白细胞 8~12/HP（参考 0~5/HP）；亚硝酸盐阴性；葡萄糖阴性。",
                    "尿常规异常，提示泌尿系感染或肾小球损伤可能，建议复查并结合临床。");
        }
        return new ReportContent(
                "尿比重 1.015~1.025；pH 5.0~7.0；蛋白 (-)；隐血 (-)；白细胞 0~3/HP（参考 0~5/HP）；亚硝酸盐阴性；葡萄糖阴性。",
                "尿常规未见明显异常。");
    }

    private ReportContent generateLiverFunction() {
        boolean abnormal = isAbnormal();
        double alt = abnormal ? round(65 + random.nextDouble() * 80, 0) : round(15 + random.nextDouble() * 25, 0);
        double ast = abnormal ? round(58 + random.nextDouble() * 70, 0) : round(18 + random.nextDouble() * 22, 0);
        return new ReportContent(
                String.format("ALT %.0f U/L（参考 7~40）；AST %.0f U/L（参考 13~35）；总胆红素 %.1f μmol/L（参考 3.4~20.5）；白蛋白 %d g/L（参考 40~55）。",
                        alt, ast, round(8 + random.nextDouble() * 10, 1), randomInt(38, 48)),
                abnormal ? "肝功能指标升高，提示肝细胞损伤可能，建议结合病史及影像学检查。" : "肝功能未见明显异常。");
    }

    private ReportContent generateRenalFunction() {
        boolean abnormal = isAbnormal();
        double crea = abnormal ? round(120 + random.nextDouble() * 80, 0) : round(55 + random.nextDouble() * 40, 0);
        return new ReportContent(
                String.format("尿素 %.1f mmol/L（参考 2.9~8.2）；肌酐 %.0f μmol/L（参考 57~111）；尿酸 %d μmol/L（参考 208~428）；eGFR %d mL/min/1.73m²。",
                        round(4 + random.nextDouble() * 5, 1), crea, randomInt(250, 380), abnormal ? randomInt(45, 75) : randomInt(85, 120)),
                abnormal ? "肾功能指标异常，建议肾脏专科进一步评估。" : "肾功能未见明显异常。");
    }

    private ReportContent generateGlucose() {
        boolean abnormal = isAbnormal();
        double fpg = abnormal ? round(7.2 + random.nextDouble() * 4, 1) : round(4.5 + random.nextDouble() * 1.8, 1);
        return new ReportContent(
                String.format("空腹血糖 %.1f mmol/L（参考 3.9~6.1）；糖化血红蛋白 %.1f%%（参考 4.0~6.0）。", fpg, round(5.0 + random.nextDouble() * 1.5, 1)),
                abnormal ? "血糖偏高，建议内分泌科进一步评估及生活方式干预。" : "血糖及糖化血红蛋白在参考范围内。");
    }

    private ReportContent generateLipidProfile() {
        boolean abnormal = isAbnormal();
        double ldl = abnormal ? round(3.8 + random.nextDouble() * 1.5, 2) : round(2.0 + random.nextDouble() * 1.2, 2);
        return new ReportContent(
                String.format("总胆固醇 %.2f mmol/L（参考 <5.2）；甘油三酯 %.2f mmol/L（参考 <1.7）；LDL-C %.2f mmol/L（参考 <3.4）；HDL-C %.2f mmol/L（参考 >1.0）。",
                        round(4.0 + random.nextDouble() * 1.5, 2), round(0.8 + random.nextDouble() * 1.5, 2), ldl, round(1.0 + random.nextDouble() * 0.6, 2)),
                abnormal ? "血脂异常，建议低脂饮食并心血管风险评估。" : "血脂未见明显异常。");
    }

    private ReportContent generateCoagulation(String key) {
        if (matches(key, "d-二聚体", "d二聚体")) {
            boolean abnormal = isAbnormal();
            double dDimer = abnormal ? round(0.8 + random.nextDouble() * 2.5, 2) : round(0.1 + random.nextDouble() * 0.4, 2);
            return new ReportContent(
                    String.format("D-二聚体 %.2f mg/L FEU（参考 <0.55）。", dDimer),
                    abnormal ? "D-二聚体升高，需排除血栓性疾病并结合临床。" : "D-二聚体未见明显升高。");
        }
        return new ReportContent(
                String.format("PT %.1f s（参考 11~14.5）；INR %.2f（参考 0.8~1.2）；APTT %.1f s（参考 25~35）；纤维蛋白原 %.2f g/L（参考 2.0~4.0）。",
                        round(11 + random.nextDouble() * 3, 1), round(0.9 + random.nextDouble() * 0.2, 2),
                        round(26 + random.nextDouble() * 6, 1), round(2.5 + random.nextDouble() * 1.2, 2)),
                isAbnormal() ? "凝血功能轻度异常，建议复查。" : "凝血功能未见明显异常。");
    }

    private ReportContent generateThyroid() {
        boolean abnormal = isAbnormal();
        double tsh = abnormal ? round(0.1 + random.nextDouble() * 0.2, 2) : round(1.5 + random.nextDouble() * 2.5, 2);
        if (abnormal && random.nextBoolean()) {
            tsh = round(5.5 + random.nextDouble() * 8, 2);
        }
        return new ReportContent(
                String.format("TSH %.2f mIU/L（参考 0.27~4.2）；FT3 %.1f pmol/L（参考 3.1~6.8）；FT4 %.1f pmol/L（参考 12~22）。",
                        tsh, round(4.5 + random.nextDouble() * 1.5, 1), round(14 + random.nextDouble() * 5, 1)),
                abnormal ? "甲状腺功能异常，建议内分泌科进一步评估。" : "甲状腺功能未见明显异常。");
    }

    private ReportContent generateTumorMarker() {
        boolean abnormal = isAbnormal();
        if (abnormal) {
            return new ReportContent(
                    String.format("CEA %.1f ng/mL（参考 <5.0）；AFP %.1f ng/mL（参考 <7.0）；CA199 %d U/mL（参考 <37）。",
                            round(6 + random.nextDouble() * 12, 1), round(8 + random.nextDouble() * 15, 1), randomInt(40, 120)),
                    "部分肿瘤标志物升高，建议结合影像学及专科随访，不能单独作为诊断依据。");
        }
        return new ReportContent(
                String.format("CEA %.1f ng/mL（参考 <5.0）；AFP %.1f ng/mL（参考 <7.0）；CA199 %d U/mL（参考 <37）；PSA %.2f ng/mL（参考 <4.0）。",
                        round(1 + random.nextDouble() * 3, 1), round(2 + random.nextDouble() * 4, 1), randomInt(8, 25), round(0.5 + random.nextDouble() * 2, 2)),
                "肿瘤标志物未见明显异常。");
    }

    private ReportContent generateElectrolyte() {
        boolean abnormal = isAbnormal();
        double k = abnormal ? round(3.2 + random.nextDouble() * 0.4, 1) : round(3.8 + random.nextDouble() * 0.8, 1);
        if (abnormal && random.nextBoolean()) {
            k = round(5.3 + random.nextDouble() * 0.8, 1);
        }
        return new ReportContent(
                String.format("钾 %.1f mmol/L（参考 3.5~5.3）；钠 %d mmol/L（参考 137~147）；氯 %d mmol/L（参考 99~110）；钙 %.2f mmol/L（参考 2.11~2.52）。",
                        k, randomInt(138, 145), randomInt(100, 108), round(2.2 + random.nextDouble() * 0.25, 2)),
                abnormal ? "电解质紊乱，建议纠正并查明原因。" : "电解质未见明显异常。");
    }

    private ReportContent generateInflammationMarker(String key) {
        boolean abnormal = isAbnormal();
        if (matches(key, "降钙素原", "pct")) {
            double pct = abnormal ? round(0.8 + random.nextDouble() * 3, 2) : round(0.02 + random.nextDouble() * 0.08, 2);
            return new ReportContent(String.format("降钙素原 %.2f ng/mL（参考 <0.5）。", pct),
                    abnormal ? "降钙素原升高，提示细菌感染可能。" : "降钙素原未见明显升高。");
        }
        double crp = abnormal ? round(15 + random.nextDouble() * 60, 1) : round(1 + random.nextDouble() * 8, 1);
        return new ReportContent(String.format("C反应蛋白 %.1f mg/L（参考 0~10）。", crp),
                abnormal ? "CRP 升高，提示炎症反应，建议结合临床。" : "CRP 未见明显升高。");
    }

    private ReportContent generateHepatitisMarker() {
        return new ReportContent(
                "HBsAg (-)；Anti-HBs (+)；HBeAg (-)；Anti-HBe (+)；Anti-HBc (-)；Anti-HCV (-)。",
                isAbnormal() ? "乙肝表面抗原阳性，建议感染科进一步评估。" : "肝炎标志物未见活动性感染证据。");
    }

    private ReportContent generateBloodGas() {
        return new ReportContent(
                String.format("pH %.2f（参考 7.35~7.45）；PaCO2 %d mmHg（参考 35~45）；PaO2 %d mmHg（参考 80~100）；HCO3- %.1f mmol/L（参考 22~26）；BE %.1f mmol/L。",
                        round(7.36 + random.nextDouble() * 0.06, 2), randomInt(36, 44), randomInt(82, 98), round(23 + random.nextDouble() * 3, 1), round(-1 + random.nextDouble() * 2, 1)),
                isAbnormal() ? "血气分析轻度异常，建议结合呼吸状态评估。" : "血气分析大致正常。");
    }

    // ---------- 影像/功能类 ----------

    private ReportContent generateChestXray(String part) {
        boolean abnormal = isAbnormal();
        String site = partLabel(part, "胸部");
        if (abnormal) {
            return new ReportContent(
                    String.format("%s正位片：双肺纹理增粗、紊乱，右下肺野见斑片状模糊影，心影大小正常，双侧膈面光整，肋膈角锐利。", site),
                    "胸部 X 线考虑支气管炎或轻度炎症改变，建议结合临床及必要时 CT 复查。");
        }
        return new ReportContent(
                String.format("%s正位片：双肺野清晰，肺纹理走行自然，未见明显实变影及胸腔积液，心影大小形态正常，膈面光整。", site),
                "胸部 X 线未见明显异常。");
    }

    private ReportContent generateHeadCt(String part) {
        boolean abnormal = isAbnormal();
        String site = partLabel(part, "头颅");
        if (abnormal) {
            return new ReportContent(
                    String.format("%s CT 平扫：左侧基底节区见小片状低密度影，边界欠清，脑室系统无扩大，中线结构居中，颅骨未见异常。", site),
                    "头颅 CT 提示腔隙性脑梗死可能，建议神经内科进一步评估。");
        }
        return new ReportContent(
                String.format("%s CT 平扫：脑实质密度均匀，未见出血及占位，脑室、脑池形态正常，中线结构居中。", site),
                "头颅 CT 未见明显异常。");
    }

    private ReportContent generateChestCt(String part) {
        boolean abnormal = isAbnormal();
        if (abnormal) {
            return new ReportContent(
                    partLabel(part, "胸部") + " CT：右肺中叶见 5mm 小结节影，纵隔淋巴结未见明显肿大，胸腔无积液。",
                    "右肺小结节，建议随诊复查或进一步检查。");
        }
        return new ReportContent(
                partLabel(part, "胸部") + " CT：双肺支气管血管束走行自然，未见明显结节及实变，纵隔结构清晰，无胸腔积液。",
                "胸部 CT 未见明显异常。");
    }

    private ReportContent generateAbdominalCt(String part) {
        boolean abnormal = isAbnormal();
        if (abnormal) {
            return new ReportContent(
                    partLabel(part, "腹部") + " CT：肝右叶见小低密度灶，边界尚清，胆囊未见结石，胰腺、脾脏及双肾未见明显异常。",
                    "肝右叶低密度灶，建议增强 CT 或 MRI 进一步评估。");
        }
        return new ReportContent(
                partLabel(part, "腹部") + " CT：肝脏形态大小正常，密度均匀，胆囊、胰腺、脾脏及双肾未见明显异常，腹腔无积液。",
                "腹部 CT 未见明显异常。");
    }

    private ReportContent generateGenericCt(String type, String part) {
        return abnormalOrNormal(
                () -> new ReportContent(
                        String.format("%s%s CT：局部见稍低密度影，边界欠清，周围结构尚可（含参考值范围描述）。",
                                type, partSuffix(part)),
                        "CT 所见异常，建议结合临床及增强扫描。"),
                () -> new ReportContent(
                        String.format("%s%s CT：未见明显占位、出血及骨折征象，结构对称，密度未见异常。", type, partSuffix(part)),
                        "CT 检查未见明显异常。"));
    }

    private ReportContent generateMri(String type, String part) {
        return abnormalOrNormal(
                () -> new ReportContent(
                        partLabel(part, type) + " MRI：T2 加权像见斑片状高信号，边界模糊，增强扫描轻度强化。",
                        "MRI 提示异常信号，建议专科进一步评估。"),
                () -> new ReportContent(
                        partLabel(part, type) + " MRI：各序列信号未见明显异常，结构对称，无占位及水肿表现。",
                        "MRI 未见明显异常。"));
    }

    private ReportContent generateUltrasound(String type, String part) {
        String site = partLabel(part, extractOrgan(part, type));
        return abnormalOrNormal(
                () -> new ReportContent(site + "超声：回声增粗、增强，可见散在强回声光点，后方无声影。",
                        "超声提示实质性病变可能，建议进一步检查。"),
                () -> new ReportContent(site + "超声：大小形态正常，回声均匀，血流信号未见明显异常，未见占位及积液。",
                        "超声检查未见明显异常。"));
    }

    private ReportContent generateEchocardiography() {
        return abnormalOrNormal(
                () -> new ReportContent(
                        "心脏超声：左室舒张功能减低，EF 约 52%（参考 >55%），二尖瓣轻度反流，余瓣膜未见明显异常。",
                        "心脏超声提示舒张功能减低，建议心血管专科评估。"),
                () -> new ReportContent(
                        String.format("心脏超声：各房室大小正常，室壁运动协调，EF %d%%（参考 >55%%），瓣膜形态及启闭正常，无心包积液。", randomInt(58, 68)),
                        "心脏超声未见明显异常。"));
    }

    private ReportContent generateGastroscopy() {
        return abnormalOrNormal(
                () -> new ReportContent(
                        "胃镜：食管下段黏膜充血水肿，胃窦部见散在糜烂灶，未见明显溃疡及肿物，十二指肠球部正常。",
                        "慢性非萎缩性胃炎伴糜烂，建议消化内科治疗及幽门螺杆菌检测。"),
                () -> new ReportContent(
                        "胃镜：食管、贲门、胃底、胃体、胃窦及十二指肠黏膜色泽正常，未见溃疡、肿物及活动性出血。",
                        "上消化道内镜未见明显异常。"));
    }

    private ReportContent generateColonoscopy() {
        return abnormalOrNormal(
                () -> new ReportContent(
                        "肠镜：降结肠见 0.6cm 息肉样隆起，表面光滑，其余肠段黏膜充血，血管纹理清晰。",
                        "结肠息肉，建议病理检查及定期复查。"),
                () -> new ReportContent(
                        "肠镜：全结肠黏膜光滑，血管纹理清晰，未见息肉、溃疡及肿物。",
                        "结肠镜未见明显异常。"));
    }

    private ReportContent generatePulmonaryFunction() {
        return abnormalOrNormal(
                () -> new ReportContent(
                        String.format("FVC %.2f L（预计值 85%%）；FEV1 %.2f L（预计值 72%%）；FEV1/FVC %d%%（参考 >70%%）。",
                                round(2.5 + random.nextDouble() * 0.8, 2), round(1.6 + random.nextDouble() * 0.5, 2), randomInt(62, 68)),
                        "肺功能提示轻度阻塞性通气功能障碍。"),
                () -> new ReportContent(
                        String.format("FVC %.2f L（预计值 %d%%）；FEV1 %.2f L（预计值 %d%%）；FEV1/FVC %d%%（参考 >70%%）。",
                                round(3.0 + random.nextDouble() * 0.8, 2), randomInt(85, 105),
                                round(2.4 + random.nextDouble() * 0.6, 2), randomInt(88, 102), randomInt(75, 82)),
                        "肺通气功能正常。"));
    }

    private ReportContent generateBoneDensity(String part) {
        String site = partLabel(part, "腰椎");
        return abnormalOrNormal(
                () -> new ReportContent(site + " 骨密度：T 值 -2.6（参考 >-1.0），骨量减少。",
                        "骨密度降低，符合骨质疏松，建议骨科或内分泌科评估。"),
                () -> new ReportContent(site + " 骨密度：T 值 -0.8（参考 >-1.0），骨量正常。",
                        "骨密度未见明显异常。"));
    }

    private ReportContent generatePathology(String type, String part) {
        return abnormalOrNormal(
                () -> new ReportContent(
                        partLabel(part, type) + " 病理：镜下见腺体结构紊乱，局部细胞异型性，核分裂象偶见。",
                        "病理提示异型增生，建议专科会诊及进一步治疗。"),
                () -> new ReportContent(
                        partLabel(part, type) + " 病理：送检组织镜下结构正常，未见异型细胞及恶性证据。",
                        "病理未见恶性病变。"));
    }

    private ReportContent generateBloodPressure() {
        int sbp = randomInt(110, 130);
        int dbp = randomInt(70, 82);
        if (isAbnormal()) {
            sbp = randomInt(145, 165);
            dbp = randomInt(92, 102);
            return new ReportContent(
                    String.format("血压监测：收缩压 %d mmHg，舒张压 %d mmHg（参考 <140/90 mmHg），平均动脉压 %.0f mmHg。",
                            sbp, dbp, (sbp + 2 * dbp) / 3.0),
                    "血压偏高，建议高血压评估及生活方式干预。");
        }
        return new ReportContent(
                String.format("血压监测：收缩压 %d mmHg，舒张压 %d mmHg（参考 <140/90 mmHg）。", sbp, dbp),
                "血压在参考范围内。");
    }

    private ReportContent generateOphthalmology(String key) {
        if (matches(key, "眼压")) {
            double iop = isAbnormal() ? round(22 + random.nextDouble() * 8, 1) : round(12 + random.nextDouble() * 8, 1);
            return new ReportContent(String.format("眼压 %.1f mmHg（参考 10~21）。", iop),
                    iop > 21 ? "眼压升高，建议青光眼相关评估。" : "眼压正常。");
        }
        return new ReportContent("视野检查：未见明显暗点及视野缺损。", "视野检查未见明显异常。");
    }

    private ReportContent generateAudiometry() {
        return abnormalOrNormal(
                () -> new ReportContent("纯音测听：双耳高频听阈升高 25~35 dB HL，语频区听阈基本正常。",
                        "高频听力下降，建议耳鼻喉科进一步评估。"),
                () -> new ReportContent("纯音测听：双耳各频率听阈在正常范围（0~25 dB HL）。",
                        "听力检查未见明显异常。"));
    }

    private ReportContent generateDefault(String type, String part, String indication) {
        String intro = StringUtils.hasText(indication) ? "临床指征：" + indication + "。" : "";
        return abnormalOrNormal(
                () -> new ReportContent(
                        intro + String.format(" %s%s：局部见轻度异常信号/密度改变，建议结合其他检查。", type, partSuffix(part)),
                        "检查所见轻度异常，建议随访或进一步评估。"),
                () -> new ReportContent(
                        intro + String.format(" %s%s：各参数在参考范围内，未见明显器质性异常。", type, partSuffix(part)),
                        "结合临床，本次检查未见明显异常。"));
    }

    // ---------- 工具方法 ----------

    private ReportContent abnormalOrNormal(Supplier<ReportContent> abnormal, Supplier<ReportContent> normal) {
        return isAbnormal() ? abnormal.get() : normal.get();
    }

    /** 约 10%~30% 概率出现异常 */
    private boolean isAbnormal() {
        double threshold = 0.10 + random.nextDouble() * 0.20;
        return random.nextDouble() < threshold;
    }

    private int randomInt(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private boolean matches(String key, String... keywords) {
        for (String keyword : keywords) {
            if (key.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String partLabel(String part, String fallback) {
        return StringUtils.hasText(part) ? part : fallback;
    }

    private String partSuffix(String part) {
        return StringUtils.hasText(part) ? "（" + part + "）" : "";
    }

    private String extractOrgan(String part, String type) {
        if (StringUtils.hasText(part)) {
            return part;
        }
        if (type.contains("腹")) {
            return "腹部";
        }
        if (type.contains("心")) {
            return "心脏";
        }
        return "检查部位";
    }

    private record ReportContent(String reportText, String conclusion) {
    }
}
