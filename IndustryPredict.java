import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.py.Pinyin;
import com.hankcs.hanlp.suggest.Suggester;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mh.tm.Server.relationOfScopeAndProduct.read;

public class IndustryPredict {

    public static void main(String[] args) throws IOException{
        // 行业信息处理调用代码
        Map<String, String> mapName = new HashMap<>();
        Map<String, String> mapContent = new HashMap<>();
        industryInformationProcess(mapName, mapContent);

        // 模拟通过网络传递过来的营业范围或经营范围
        String filePath = "data/scopeFromES/company_industry_scope_1w.csv";  // Csv文件操作
        List<String[]> scopeArray = read(filePath);
        // int testId = 70;
        int maxCount = 0;
        String maxStr = null;
        for (int testId = 0; testId < scopeArray.size(); ++testId) {
            String scopeStr = scopeArray.get(testId)[3];  // 经营范围参数
            if (scopeStr == null || "".equalsIgnoreCase(scopeStr))  // 经营范围空
                continue;
            // 如果是长文本，抽取摘要
            // 对营业范围、经营范围或文本摘要进行清洗，构造短文本
            String shortText = scopeWash(scopeStr);
            if (shortText == null || "".equalsIgnoreCase(shortText)) // 清洗后成为空
                continue;
            System.out.println(testId);
            System.out.println(scopeStr);
            System.out.println(shortText);
            List<String> indusLst = textRecommend(shortText, mapName, mapContent);
            System.out.println(indusLst);
            if (maxCount < scopeStr.length()){
                maxCount = scopeStr.length();
                maxStr = scopeStr;
            }
        }
        System.out.println("经营范围最大长度：" + String.valueOf(maxCount));
        System.out.println("经营范围："+ maxStr);
    }

    /**
     * 清洗
     * @param scopeStr
     * @return
     */
    public static String scopeWash(String scopeStr){
        return scopeStr.replaceAll("[ \\r\\n\\t]", "")
                .replaceAll("(null|NULL|Null)", "")
                .replaceAll("(一般|许可)经营项目：", "")
                .replaceAll("经营(项目|范围)：", "")
                .replaceAll("〓|无 |其他|其它|服务|管理|建设|加工|分包|经营|范围|产品|技术|相关|开展|成员|诚信", "")
                .replaceAll("[（(](.*?)[)）]", "")
                .replaceAll("[【](.*?)[】]", "")
                .replaceAll("[a-zA-Z0-9\\-]+", "");
    }

    /**
     * 行业信息处理，构造两个Map
     * @param mapName 行业门类与名称
     * @param mapContent  恒业门类与详情
     */
    public static void industryInformationProcess(
            Map<String, String> mapName,
            Map<String, String> mapContent) throws IOException{

        File filename = new File("data/text/industry");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "utf-8"));
        String line = null;
        while((line = br.readLine())!=null){
            if (line.trim().length() < 2) continue;
            String[] tmpLst = removeBorderBlank(line).split(" ");
            mapContent.put(tmpLst[0], tmpLst[1]);
            String tmpName = removeBorderBlank(tmpLst[1]).split(":")[0];
            mapName.put(tmpLst[0], tmpName);
        }
        br.close();
    }

    /**
     * 文本推荐(句子级别，从一系列句子中挑出与输入句子最相似的那一个)
     */
    public static List<String> textRecommend(String shortText, Map<String, String> mapName, Map<String, String> mapContent)
    {
        List<String> indusLst = new ArrayList<>();

        Suggester suggester = new Suggester();
        for (String title : mapContent.values())
        {
            suggester.addSentence(title);
        }

        List<String> meaningLst = suggester.suggest(shortText, 1);   // 语义
        // 汉字转拼音
        String pinyinStr = getPinYin(shortText);
        List<String> pinyinLst = suggester.suggest(pinyinStr, 1);   // 拼音

        if (!meaningLst.get(0).equalsIgnoreCase(pinyinLst.get(0))){
            // 遍历mapName，考察 pinyinLst, 返回两个行业预测值
            for (Map.Entry<String, String> entry: mapName.entrySet()){
                if (entry.getValue().length() > pinyinLst.get(0).length())
                    continue;
                String pinyinAnswerStr = pinyinLst.get(0).substring(0, entry.getValue().length());
                if (pinyinAnswerStr.equalsIgnoreCase(entry.getValue())){
                    indusLst.add(entry.getKey() + " " + entry.getValue());
                    break;
                }
            }
        }
        // 遍历mapName, 考察meaningLst, 返回一个行业预测值
        for (Map.Entry<String, String> entry: mapName.entrySet()){
            if (entry.getValue().length() > meaningLst.get(0).length())
                continue;
            String meaningAnswerStr = meaningLst.get(0).substring(0, entry.getValue().length());
            if (meaningAnswerStr.equalsIgnoreCase(entry.getValue())){
                indusLst.add(entry.getKey() + " " + entry.getValue());
                break;
            }
        }

        return indusLst;
    }

    /**
     * 获取拼音字符串
     * @param shortText
     * @return
     */
    public static String getPinYin(String shortText){
        List<Pinyin> pinyinList = HanLP.convertToPinyinList(shortText);
        String pinyinStr = null;
        for (Pinyin pinyin: pinyinList)
            pinyinStr += pinyin.getPinyinWithoutTone();

        return pinyinStr;
    }

    /**
     * 去除字符串中头部和尾部所包含的空格（包括:空格(全角，半角)、制表符、换页符等）
     * @params
     * @return
     */
    public static String removeBorderBlank(String s){
        String result = "";
        if(null != s && !"".equals(s)){
            result = s.replaceAll("^[　 \\s*]*", "").replaceAll("[　 \\s*]*$", "");
        }
        return result;
    }
}
