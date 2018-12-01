import com.alibaba.fastjson.JSONArray;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.io.IOUtil;
import com.hankcs.hanlp.mining.word2vec.WordVectorModel;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import com.csvreader.*;
import com.xm.Similarity;

public class BusinessScopeAndProduct {
    private static final String MODEL_FILE_NAME = "data/test/word2vec_wiki.txt";

    public static void main(String[] args) throws IOException {
        WordVectorModel wordVectorModelWiki = trainOrLoadModelSimilarity();  // 加载维基百科词向量模型
        String filePath = "data/scopeFromES/company_industry_scope_1w.csv";  // Csv文件操作
        // write();
        List<String[]> scopeArray = read(filePath);

        // 测试代码
        int testId = 7519;
        final double warningVal = 0.34;
        // 通过网络传递过来的参数
        String tmpProduct = "手机";  // 产品参数
        String scopeStr = scopeArray.get(testId)[3];  // 经营范围参数
        String returnString;  // 返回结果字符串
        List<List<String>> riskCompanyInfo = new ArrayList<>();

        //  测试1：根据产品和经营范围判断经营合法性的方法
        System.out.println(scopeArray.get(testId)[0]);
        System.out.println(scopeArray.get(testId)[1]);
        System.out.println(scopeArray.get(testId)[2]);
        System.out.println(scopeArray.get(testId)[3]);
        List<String> riskStrLst = productScopeFunc(tmpProduct, scopeStr, wordVectorModelWiki, warningVal);  // 合法性预测
        for (int idx_ = 0; idx_ < riskStrLst.size(); ++idx_){
            System.out.println(riskStrLst.get(idx_));
        }

//        // 测试2：根据产品和经营范围判断经营合法性，遍历测试全部公司，返回预测产品超出经营范围的公司信息
//        riskCompanyInfo = productScopeFuncAll(tmpProduct, scopeArray, wordVectorModelWiki, warningVal);
//        for (int idx_ = 0; idx_ < riskCompanyInfo.get(0).size(); ++idx_){
//            System.out.println(riskCompanyInfo.get(0).get(idx_));
//            System.out.println(riskCompanyInfo.get(1).get(idx_));
//            System.out.println(riskCompanyInfo.get(2).get(idx_));
//            System.out.println(riskCompanyInfo.get(3).get(idx_));
//            System.out.println(riskCompanyInfo.get(4).get(idx_));
//            System.out.println();
//        }
//        System.out.println("存在风险的公司累计：" + riskCompanyInfo.get(0).size() + "家。");

//        // 测试3：根据产品参数和公司名称，预测产品是否存在经营风险
//        String tmpCompany = "建湖县宝塔镇晓青诚信手机店";
//        List<List<String>> riskOfProductCompany = new ArrayList<>();
//        riskOfProductCompany = productCompanyFunc(tmpProduct, tmpCompany, scopeArray, wordVectorModelWiki, warningVal);
//        for (int idx_ = 0; idx_ < riskOfProductCompany.get(0).size(); ++idx_){
//            System.out.println("公司名称：" + riskOfProductCompany.get(0).get(idx_));
//            System.out.println("行业类型：" + riskOfProductCompany.get(1).get(idx_));
//            System.out.println("经营范围：" + riskOfProductCompany.get(2).get(idx_));
//            System.out.println("风险指数：" + riskOfProductCompany.get(3).get(idx_));
//            System.out.println("特征串：\t" + riskOfProductCompany.get(4).get(idx_));
//            System.out.println("预测结果：" + riskOfProductCompany.get(5).get(idx_));
//        }

//        // 测试4: 检索出所有经营范围为空的公司
//        List<List<String>> scopeNullLst = new ArrayList<>();
//        scopeNullLst = productAndScopeNull(scopeArray);
//        for (int i_ = 0; i_ < scopeNullLst.get(0).size(); ++i_){
//            System.out.println("机构：\t" + scopeNullLst.get(0).get(i_));
//            // System.out.println("行业：\t" + scopeNullLst.get(1).get(i_));
//            System.out.println();
//        }
//        System.out.println("累计：" + scopeNullLst.get(0).size());

        // 返回预测结果的代码放此处
    }

    /** 测试1：根据产品和经营范围判断经营合法性的方法
     *
     * @param tmpProduct
     * @param scopeString
     * @param wordVectorModelWiki
     * @param warningValue
     * @return [空|预警|合法, 风险指数, 特征串]
     * @throws IOException
     */
    public static List<String> productScopeFunc(String tmpProduct,
                                          String scopeString,
                                          WordVectorModel wordVectorModelWiki,
                                          final double warningValue) throws IOException {
        // 测试数据
        String riskStr = "空";
        List<String> returnLst = new ArrayList<>();

        // 经营范围分词，去除停用词，括号内容；提取关键词，构造特征串
        if (scopeString.replaceAll("[ \\r\\n\\t]", "").isEmpty()){
            returnLst.add(riskStr);
            returnLst.add("");
            returnLst.add("");
            return returnLst;
        }
        List<String> featureStr = getKeyword(scopeString
                .replaceAll("(一般|许可)经营项目：", "")
                .replaceAll("经营(项目|范围)：", "")
                .replaceAll("无 |批发|零售|销售|建设|加工|服务|分包|经营|范围|产品|技术|相关|开展|成员|诚信", "")
                .replaceAll("[（(](.*?)[)）]", "")
                .replaceAll("[【](.*?)[】]", "")
                .replaceAll("[a-zA-Z0-9\\-]+", ""), 5);
        // 计算五种相似度及其平均值，并累记平均值小于0.1的种类数目
        int count;
        count = 0;
//        // 根据平均值判断产品是否超出经营范围
//        List<Float> featureValWiki = computeRelationWiki(featureStr, tmpProduct, wordVectorModelWiki);
//        float meanWiki = avgOfValList(featureValWiki);
//        if (meanWiki < warningValue) count += 1;
//        List<Double> featureValCinlin = computeRelationCinlin(featureStr, tmpProduct);
//        float meanCinlin = avgOfValList(featureValCinlin);
//        if (meanCinlin < warningValue) count += 1;
//        List<Double> featureValPinyin = computeRelationPinyin(featureStr, tmpProduct);
//        float meanPinyin = avgOfValList(featureValPinyin);
//        if (meanPinyin < warningValue) count += 1;
//        List<Double> featureValConcept = computeRelationConcept(featureStr, tmpProduct);
//        float meanConcept = avgOfValList(featureValConcept);
//        if (meanConcept < warningValue) count += 1;
//        List<Double> featureValChar = computeRelationChar(featureStr, tmpProduct);
//        float meanChar = avgOfValList(featureValChar);
//        if (meanChar < warningValue) count += 1;
        double sumOfMeanVal;
        sumOfMeanVal = 0.0;
        // 依据各相似度方案的最大值，进行风险预测
        List<Float> featureValWiki = computeRelationWiki(featureStr, tmpProduct, wordVectorModelWiki);
        float meanWiki = Collections.max(featureValWiki);
        sumOfMeanVal += meanWiki;
        if (meanWiki < warningValue) count += 1;

        List<Double> featureValCinlin = computeRelationCinlin(featureStr, tmpProduct);
        float meanCinlin = maxOfValList(featureValCinlin);
        sumOfMeanVal += meanCinlin;
        if (meanCinlin < warningValue) count += 1;

        List<Double> featureValPinyin = computeRelationPinyin(featureStr, tmpProduct);
        float meanPinyin = maxOfValList(featureValPinyin);
        sumOfMeanVal += meanPinyin;
        if (meanPinyin < warningValue) count += 1;

        List<Double> featureValConcept = computeRelationConcept(featureStr, tmpProduct);
        float meanConcept = maxOfValList(featureValConcept);
        sumOfMeanVal += meanConcept;
        if (meanConcept < warningValue) count += 1;

        List<Double> featureValChar = computeRelationChar(featureStr, tmpProduct);
        float meanChar = maxOfValList(featureValChar);
        sumOfMeanVal += meanChar;
        if (meanChar < warningValue) count += 1;

        riskStr = (count > 3)?"预警":"合法";
        double indexOfRisk = (5 - sumOfMeanVal) / 5;
        float indexOfRisk2 = (float) ((indexOfRisk > 0.99) ? 0.98 : indexOfRisk * 0.99);
        returnLst.add(riskStr);
        returnLst.add(String.valueOf(indexOfRisk2));
        returnLst.add(JSONArray.toJSONString(featureStr));

//        System.out.println("特征词串:\t\t" + featureStr);
//        System.out.println("百科相似度:\t" + featureValWiki + " ==> " + meanWiki);
//        System.out.println("词林相似度:\t" + featureValCinlin + " ==> " + meanCinlin);
//        System.out.println("拼音相似度:\t" + featureValPinyin + " ==> " + meanPinyin);
//        System.out.println("概念相似度:\t" + featureValConcept + " ==> " + meanConcept);
//        System.out.println("字面相似度:\t" + featureValChar + " ==> " + meanChar);

        return returnLst;
    }

    /** 测试2：根据产品和经营范围判断经营合法性，遍历测试全部公司，返回预测产品超出经营范围的公司信息
     *
     * @param tmpProduct
     * @param scopeArray
     * @param wordVectorModelWiki
     * @param warningValue
     * @return [[机构][行业][经营范围][风险指数][特征串]]
     * @throws IOException
     */
    public static List<List<String>> productScopeFuncAll(String tmpProduct,
                                                           List<String[]> scopeArray,
                                                           WordVectorModel wordVectorModelWiki,
                                                           final double warningValue) throws IOException {
        // 测试数据
        String isRisk;
        List<List<String>> returnLst = new ArrayList<>();  // 双重列表，存储以下四个列表，作为结果返回
        List<String> companyLst = new ArrayList<>();  // 公司名称
        List<String> industryLst = new ArrayList<>();  // 行业信息
        List<String> scopeLst = new ArrayList<>();  // 经营范围
        List<String>  indexOfRiskLst = new ArrayList<>();  // 风险指数，浮点型转换成字符串再存储
        List<String> featureLst = new ArrayList<>();

        for (String[] sA: scopeArray){
            String riskStr = "空";
            List<String> featureStr;

            String scopeString = sA[3].replaceAll("[ \\r\\n\\t]", "")
                    .replaceAll("(null|NULL|Null)", "")
                    .replaceAll("(一般|许可)经营项目：", "")
                    .replaceAll("经营(项目|范围)：", "")
                    .replaceAll("无 |批发|零售|销售|建设|加工|服务|分包|经营|范围|产品|技术|相关|开展|成员|诚信", "")
                    .replaceAll("[（(](.*?)[)）]", "")
                    .replaceAll("[【](.*?)[】]", "")
                    .replaceAll("[a-zA-Z0-9\\-]+", "");
            // 经营范围为空
            if (scopeString.isEmpty()){

                String industryAndCompanyName = sA[2] + " " + sA[1];
                industryAndCompanyName = industryAndCompanyName
                        .replaceAll("[ \\r\\n\\t]", "")
                        .replaceAll("(null|NULL|Null)", "")
                        .replaceAll("(一般|许可)经营项目：", "")
                        .replaceAll("经营(项目|范围)：", "")
                        .replaceAll("无 |批发|零售|销售|建设|加工|服务|分包|经营|范围|产品|技术|相关|开展|成员|诚信", "")
                        .replaceAll("[（(](.*?)[)）]", "")
                        .replaceAll("[【](.*?)[】]", "")
                        .replaceAll("[a-zA-Z0-9\\-]+", "");

                if (industryAndCompanyName.isEmpty()){
//                    System.out.println("ID:\t\t\t" + sA[0]);
//                    System.out.println("机构:\t\t" + sA[1]);
//                    System.out.println("行业:\t\t" + sA[2]);
//                    System.out.println("经营范围:\t\t" + sA[3]);
//                    System.out.println("有无风险:\t\t" + isRisk);
//                    System.out.println();
                    continue;
                }
                // 行业字符串分词，去除停用词，括号内容；提取关键词，构造特征串
                featureStr = getKeyword(industryAndCompanyName, 5);

            }
            // 经营范围分词，去除停用词，括号内容；提取关键词，构造特征串
            else{
                featureStr = getKeyword(scopeString, 5);
            }

            //如果特征串为空，终止本轮循环，继续下一轮循环
            if (featureStr.size() == 0){
//                isRisk = "特征为空";
//                System.out.println("ID:\t\t\t" + sA[0]);
//                System.out.println("机构:\t\t" + sA[1]);
//                System.out.println("行业:\t\t" + sA[2]);
//                System.out.println("经营范围:\t\t" + sA[3]);
//                System.out.println("特征词串:\t\t" + featureStr);
//                System.out.println("有无风险:\t\t" + isRisk);
                continue;
            }

            // 计算五种相似度及其平均值，并累记平均值小于0.1的种类数目
            double sumOfMeanVal;
            sumOfMeanVal = 0.0;
            int count;
            count = 0;

//            // 依据各相似度方案的平均值，进行风险预测
//            List<Float> featureValWiki = computeRelationWiki(featureStr, tmpProduct, wordVectorModelWiki);
//            float meanWiki = avgOfValList(featureValWiki);
//            sumOfMeanVal += meanWiki;
//            if (meanWiki < warningValue) count += 1;
//
//            List<Double> featureValCinlin = computeRelationCinlin(featureStr, tmpProduct);
//            float meanCinlin = avgOfValList(featureValCinlin);
//            sumOfMeanVal += meanCinlin;
//            if (meanCinlin < warningValue) count += 1;
//
//            List<Double> featureValPinyin = computeRelationPinyin(featureStr, tmpProduct);
//            float meanPinyin = avgOfValList(featureValPinyin);
//            sumOfMeanVal += meanPinyin;
//            if (meanPinyin < warningValue) count += 1;
//
//            List<Double> featureValConcept = computeRelationConcept(featureStr, tmpProduct);
//            float meanConcept = avgOfValList(featureValConcept);
//            sumOfMeanVal += meanConcept;
//            if (meanConcept < warningValue) count += 1;
//
//            List<Double> featureValChar = computeRelationChar(featureStr, tmpProduct);
//            float meanChar = avgOfValList(featureValChar);
//            sumOfMeanVal += meanChar;
//            if (meanChar < warningValue) count += 1;

            // 依据各相似度方案的最大值，进行风险预测
            List<Float> featureValWiki = computeRelationWiki(featureStr, tmpProduct, wordVectorModelWiki);
            float meanWiki = Collections.max(featureValWiki);
            sumOfMeanVal += meanWiki;
            if (meanWiki < warningValue) count += 1;

            List<Double> featureValCinlin = computeRelationCinlin(featureStr, tmpProduct);
            float meanCinlin = maxOfValList(featureValCinlin);
            sumOfMeanVal += meanCinlin;
            if (meanCinlin < warningValue) count += 1;

            List<Double> featureValPinyin = computeRelationPinyin(featureStr, tmpProduct);
            float meanPinyin = maxOfValList(featureValPinyin);
            sumOfMeanVal += meanPinyin;
            if (meanPinyin < warningValue) count += 1;

            List<Double> featureValConcept = computeRelationConcept(featureStr, tmpProduct);
            float meanConcept = maxOfValList(featureValConcept);
            sumOfMeanVal += meanConcept;
            if (meanConcept < warningValue) count += 1;

            List<Double> featureValChar = computeRelationChar(featureStr, tmpProduct);
            float meanChar = maxOfValList(featureValChar);
            sumOfMeanVal += meanChar;
            if (meanChar < warningValue) count += 1;


            riskStr = (count > 3)?"预警":"合法";
            if (riskStr.compareTo("预警") == 0) {
                // 计算风险指数
                double indexOfRisk = (5 - sumOfMeanVal) / 5;
                float indexOfRisk2 = (float)((indexOfRisk > 0.99)? 0.98: indexOfRisk * 0.99);
                companyLst.add(sA[1]);
                industryLst.add(sA[2]);
                scopeLst.add(sA[3]);
                indexOfRiskLst.add(String.valueOf(indexOfRisk2));
                featureLst.add(featureStr.toString());

//                System.out.println("ID:\t\t\t" + sA[0]);
//                System.out.println("机构:\t\t" + sA[1]);
//                System.out.println("行业:\t\t" + sA[2]);
//                System.out.println("经营范围:\t\t" + sA[3]);
//                System.out.println("scopeStr:\t\t" + scopeString);
//                System.out.println("特征词串:\t\t" + featureStr);
//                System.out.println("百科相似度:\t" + featureValWiki + " ==> " + meanWiki);
//                System.out.println("词林相似度:\t" + featureValCinlin + " ==> " + meanCinlin);
//                System.out.println("拼音相似度:\t" + featureValPinyin + " ==> " + meanPinyin);
//                System.out.println("概念相似度:\t" + featureValConcept + " ==> " + meanConcept);
//                System.out.println("字面相似度:\t" + featureValChar + " ==> " + meanChar);
//                System.out.println("有无风险:\t\t" + riskStr + indexOfRisk2);
//                System.out.println();
            }
        }

//        System.out.println("预测出风险的公司: " + companyLst.size() + " 家");
//        System.out.println();

        returnLst.add(companyLst);
        returnLst.add(industryLst);
        returnLst.add(scopeLst);
        returnLst.add(indexOfRiskLst);
        returnLst.add(featureLst);

        return returnLst;
    }

    /** 测试3：根据产品参数和公司名称，预测产品是否存在经营风险
     *
     * @param tmpProduct
     * @param companyName
     * @param scopeArray
     * @param wordVectorModelWiki
     * @param warningValue
     * @return [[机构][行业][经营范围][风险指数][特征串][空|预警|合法]], 与公司名称匹配的公司风险情况列表
     * @throws IOException
     */
    public static List<List<String>> productCompanyFunc(String tmpProduct,
                                                         String companyName,
                                                         List<String[]> scopeArray,
                                                         WordVectorModel wordVectorModelWiki,
                                                         final double warningValue) throws IOException {
        // 测试数据
        List<List<String>> returnLst = new ArrayList<>();  // 双重列表，存储以下四个列表，作为结果返回
        List<String> companyLst = new ArrayList<>();  // 公司名称
        List<String> industryLst = new ArrayList<>();  // 行业信息
        List<String> scopeLst = new ArrayList<>();  // 经营范围
        List<String> indexOfRiskLst = new ArrayList<>();  // 风险指数，浮点型转换成字符串再存储
        List<String> featureLst = new ArrayList<>();  // 特征串列表
        List<String> riskLst = new ArrayList<>();  // 风险情况列表

        for (String[] sA: scopeArray){
            // 根据companyName和sA[1]，过滤掉其他公司
            if (!isCompanyMatch(companyName, sA[1]))
                continue;

            String riskStr = "空";
            List<String> featureStr;

            String scopeString = sA[3].replaceAll("[ \\r\\n\\t]", "")
                    .replaceAll("(null|NULL|Null)", "")
                    .replaceAll("(一般|许可)经营项目：", "")
                    .replaceAll("经营(项目|范围)：", "")
                    .replaceAll("无 |批发|零售|销售|建设|加工|服务|分包|经营|范围|产品|技术|相关|开展|成员|诚信", "")
                    .replaceAll("[（(](.*?)[)）]", "")
                    .replaceAll("[【](.*?)[】]", "")
                    .replaceAll("[a-zA-Z0-9\\-]+", "");
            // 经营范围为空
            if (scopeString.isEmpty()){

                String industryAndCompanyName = sA[2] + " " + sA[1];
                industryAndCompanyName = industryAndCompanyName
                        .replaceAll("[ \\r\\n\\t]", "")
                        .replaceAll("(null|NULL|Null)", "")
                        .replaceAll("(一般|许可)经营项目：", "")
                        .replaceAll("经营(项目|范围)：", "")
                        .replaceAll("无 |批发|零售|销售|建设|加工|服务|分包|经营|范围|产品|技术|相关|开展|成员|诚信", "")
                        .replaceAll("[（(](.*?)[)）]", "")
                        .replaceAll("[【](.*?)[】]", "")
                        .replaceAll("[a-zA-Z0-9\\-]+", "");

                // 行业字符串分词，去除停用词，括号内容；提取关键词，构造特征串
                featureStr = getKeyword(industryAndCompanyName, 5);

            }
            // 经营范围分词，去除停用词，括号内容；提取关键词，构造特征串
            else{
                featureStr = getKeyword(scopeString, 5);
            }

            //如果特征串为空，终止本轮循环，继续下一轮循环
            if (featureStr.size() == 0){
                companyLst.add(sA[1]);
                industryLst.add(sA[2]);
                scopeLst.add(sA[3]);
                indexOfRiskLst.add("");
                featureLst.add(featureStr.toString());
                riskLst.add(riskStr);
                continue;
            }

            // 计算五种相似度及其平均值，并累记平均值小于0.1的种类数目
            double sumOfMeanVal;
            sumOfMeanVal = 0.0;
            int count;
            count = 0;

            // 依据各相似度方案的最大值，进行风险预测
            List<Float> featureValWiki = computeRelationWiki(featureStr, tmpProduct, wordVectorModelWiki);
            float meanWiki = Collections.max(featureValWiki);
            sumOfMeanVal += meanWiki;
            if (meanWiki < warningValue) count += 1;

            List<Double> featureValCinlin = computeRelationCinlin(featureStr, tmpProduct);
            float meanCinlin = maxOfValList(featureValCinlin);
            sumOfMeanVal += meanCinlin;
            if (meanCinlin < warningValue) count += 1;

            List<Double> featureValPinyin = computeRelationPinyin(featureStr, tmpProduct);
            float meanPinyin = maxOfValList(featureValPinyin);
            sumOfMeanVal += meanPinyin;
            if (meanPinyin < warningValue) count += 1;

            List<Double> featureValConcept = computeRelationConcept(featureStr, tmpProduct);
            float meanConcept = maxOfValList(featureValConcept);
            sumOfMeanVal += meanConcept;
            if (meanConcept < warningValue) count += 1;

            List<Double> featureValChar = computeRelationChar(featureStr, tmpProduct);
            float meanChar = maxOfValList(featureValChar);
            sumOfMeanVal += meanChar;
            if (meanChar < warningValue) count += 1;

            riskStr = (count > 3)?"预警":"合法";
            riskLst.add(riskStr);
            // 计算风险指数
            double proportionOfRisk = (5 - sumOfMeanVal) / 5;
            float indexOfRisk = (float)((proportionOfRisk > 0.99)? 0.98: proportionOfRisk * 0.99);
            if (riskStr.compareTo("预警") == 0) { indexOfRiskLst.add(String.valueOf(indexOfRisk)); }
            else{ indexOfRiskLst.add(""); }  // 合法，设置风险指数为空

            companyLst.add(sA[1]);
            industryLst.add(sA[2]);
            scopeLst.add(sA[3]);
            featureLst.add(featureStr.toString());

            System.out.println("Wiki:" + featureValWiki);
            System.out.println("Cilin:" + featureValCinlin);
            System.out.println("Pinyin:" + featureValPinyin);
            System.out.println("Concept:" + featureValConcept);
            System.out.println("Char:" + featureValChar);
        }

        returnLst.add(companyLst);
        returnLst.add(industryLst);
        returnLst.add(scopeLst);
        returnLst.add(indexOfRiskLst);
        returnLst.add(featureLst);
        returnLst.add(riskLst);

        return returnLst;
    }

    /** 测试4：遍历全部数据，返回经营范围为空的所有公司
     *
     * @param scopeArray 经营范围
     * @return [[机构][行业]]
     * @throws IOException
     */
    public static List<List<String>> productAndScopeNull(List<String[]> scopeArray) throws IOException {
        List<List<String>> returnLst = new ArrayList<>();
        List<String> companyLst = new ArrayList<>();
        List<String> industryLst = new ArrayList<>();

        for (String[] sA : scopeArray) {
            if (sA[3].replaceAll("[ \\r\\n\\t]", "").isEmpty()) {
                companyLst.add(sA[1]);
                industryLst.add(sA[2]);
//                System.out.println("ID:\t\t\t" + sA[0]);
//                System.out.println("机构:\t\t" + sA[1]);
//                System.out.println("行业:\t\t" + sA[2]);
//                System.out.println("经营范围:\t\t" + sA[3]);
//                System.out.println("有无风险:\t\t" + "空");
//                System.out.println();
            }
        }
        returnLst.add(companyLst);
        returnLst.add(industryLst);

        return returnLst;
    }

    /** 判断输入的公司名称1是否与企业库列表当前公司名称匹配
     *
     * @param companyOfInput 用户输入的名称
     * @param companyOfList 企业库列表中的公司名称
     * @return 是否匹配 true or false
     */
    public static boolean isCompanyMatch(String companyOfInput, String companyOfList){
        boolean flagOfMatch = true;
        for (int idx = 0; idx < companyOfInput.length(); ++idx){
            if (!companyOfList.contains(String.valueOf(companyOfInput.charAt(idx)))){
                flagOfMatch = false;
                break;
            }
        }
        return flagOfMatch;
    }

    // 计算列表平均值
    static <T extends Number> float avgOfValList(List<T> arr){
        Double sum = 0.0;
        Double meanVal;
        for (int i = 0; i < arr.size(); i++){
            sum += arr.get(i).doubleValue();
        }
        meanVal = sum / arr.size();
        return meanVal.floatValue();
    }

    // 计算列表最大值
    static <T extends Number> float maxOfValList(List<T> arr){
        Double maxVal = 0.0;
        for (int i = 0; i < arr.size(); i++){
            if (maxVal < arr.get(i).doubleValue())
                maxVal = arr.get(i).doubleValue();
        }
        return maxVal.floatValue();
    }

    // 基于维基百科模型计算相似度
    static List<Float> computeRelationWiki(List<String> featureStr, String productStr, WordVectorModel wordVectorModel) throws IOException{
        List<Float> featureVal = new ArrayList<>();
        for (String s: featureStr)
            featureVal.add(wordVectorModel.similarity(s, productStr));

        return featureVal;
    }

    // 词林相似度
    static List<Double> computeRelationCinlin(List<String> featureStr, String productStr) throws IOException{

        List<Double> cilinSimiList = new ArrayList<>();

        for (String word1: featureStr){
            cilinSimiList.add(Similarity.cilinSimilarity(word1, productStr));
        }

        return cilinSimiList;
    }

    // 拼音相似度
    static List<Double> computeRelationPinyin(List<String> featureStr, String productStr) throws IOException{

        List<Double> pinyinSimiList = new ArrayList<>();

        for (String word1: featureStr){
            pinyinSimiList.add(Similarity.pinyinSimilarity(word1, productStr));
        }

        return pinyinSimiList;
    }

    // 概念相似度
    static List<Double> computeRelationConcept(List<String> featureStr, String productStr) throws IOException{

        List<Double> conceptSimiList = new ArrayList<>();

        for (String word1: featureStr){
            conceptSimiList.add(Similarity.conceptSimilarity(word1, productStr));
        }

        return conceptSimiList;
    }

    // 字面相似度
    static List<Double> computeRelationChar(List<String> featureStr, String productStr) throws IOException{

        List<Double> charBasedSimiList = new ArrayList<>();

        for (String word1:featureStr) {
            charBasedSimiList.add(Similarity.charBasedSimilarity(word1, productStr));
        }

        return charBasedSimiList;
    }

    // 获取经营范围的关键词串
    static List<String> getKeyword(String text, int count_) {
        if (count_ < 3) count_ = 3;
        List<String> keywordList = HanLP.extractKeyword(text, count_);

        return keywordList;
    }

    public static List<String[]> read(String filePath) throws IOException{
        CsvReader csvReader = new CsvReader(filePath, ',', Charset.forName("UTF-8"));
        // csvReader.readHeaders(); //路过表头
        List<String[]> scopeArray = new ArrayList<String[]>();

        while(csvReader.readRecord()){
            if (csvReader.getCurrentRecord() == 0) {
                continue;
            }
            String[] tmpStr = csvReader.getValues();
            scopeArray.add(tmpStr);
        }
        csvReader.close();

        return scopeArray;
    }

    static WordVectorModel trainOrLoadModelSimilarity() throws IOException
    {
        if (IOUtil.isFileExisted(MODEL_FILE_NAME)){
            return loadModel();
        }
        else {
            System.err.println("语料不存在，请阅读文档了解语料获取与格式：https://github.com/hankcs/HanLP/wiki/word2vec");
            System.exit(1);
            return null;
        }
    }

    static WordVectorModel loadModel() throws IOException
    {
        return new WordVectorModel(MODEL_FILE_NAME);
    }
}
