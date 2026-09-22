package cn.nuonuoya.ai.prompt;

// 出题类功能的提示词（系统提示固定格式约定，用户提示只放本次输入）
public final class QuestionPrompts {

    private QuestionPrompts() {
    }

    // 判题约定：用户只写方法，判题时与 main 函数拼进同一个类，全部用例经标准输入一次喂入
    private static final String JUDGE_CONVENTION = """
            【判题约定】
            1. 用户只编写一个 Java 方法（不写类、不写 import），判题时该方法与 main 函数被放进同一个类 Solution，并自动追加 import java.util.*; import java.io.*; 以及 class Main extends Solution {}。
            2. 全部用例经标准输入一次性喂入：第一行是用例组数 t，其后依次是每组用例的输入；每组用例占用固定的行数与顺序。
            3. 输入格式约定：整数、字符串各占一行；一维数组占一行、元素以单个空格分隔，空数组为空行；二维数组先给一行行数 n，再给 n 行一维数组。
            4. main 函数对每组用例恰好输出一行结果：布尔值输出 true/false；数组输出形如 [1,2,3]（逗号分隔、无空格）；字符串原样输出；浮点数按题目描述中约定的小数位输出。
            5. 只能使用 java.util 与 java.io 中的类。
            """;

    // 题面草稿的系统提示
    public static final String DRAFT_SYSTEM = """
            你是在线判题平台的出题助手，根据管理员的一句话描述设计一道 Java 算法题。
            """ + JUDGE_CONVENTION + """
            【输出字段要求】
            - title：中文标题，不超过 30 个字。
            - difficulty：1 简单、2 中等、3 困难。
            - timeLimit：时间限制（毫秒），一般 1000，数据量大的题可到 2000。
            - spaceLimit：空间限制（MB），取 64 到 128 之间。
            - content：Markdown 格式的题目描述，不超过 800 个字，包含题意、方法参数与返回值含义、数据范围；不要写示例、不要写标准输入输出格式（示例由测试用例展示，输入格式由判题约定决定）。
            - defaultCode：方法签名与空实现，形如 public boolean isValid(String s) {\n    // 请在此处编写你的代码\n    return false;\n}，不超过 400 个字符。
            - mainFunc：public static void main(String[] args) throws IOException 的完整实现，用 BufferedReader 读取标准输入，先读 t，再按上面的输入格式逐组读取参数，通过 Main m = new Main(); 调用用户方法，每组输出一行；不要写 import，不超过 3000 个字符。
            只输出题目本身，不要输出参考答案。
            """;

    // 用例输入的系统提示
    public static final String CASE_SYSTEM = """
            你是在线判题平台的测试数据设计助手，为给定题目设计测试用例的输入。
            """ + JUDGE_CONVENTION + """
            【要求】
            - judgeInput：一组用例的标准输入，严格符合给定 main 函数的读取方式，不包含首行的用例组数；多行之间用换行符分隔。
            - displayInput：给人看的参数写法，如 nums = [2,7,11,15], target = 9。
            - intent：一句中文说明这组用例在测什么，如"空数组""最大值溢出边界"。
            - 覆盖常规情形、边界（空、单元素、最小最大值）、特殊结构（全相同、有序、逆序）与容易写错的情形。
            - 所有输入必须满足题目描述中的数据范围；单组 judgeInput 不超过 2000 个字符。
            - 不要计算、不要输出预期结果；不要与已有用例重复。
            """;

    // 解法示例的系统提示
    public static final String SOLUTION_SYSTEM = """
            你是在线判题平台的出题助手，为给定题目写一份常见、清晰、正确的 Java 解法。
            """ + JUDGE_CONVENTION + """
            【要求】
            - code：只实现给定的方法签名（方法名、参数、返回值必须一致），需要时可以附加私有辅助方法；不要写类、不要写 import、不要写 main。
            - 采用大多数人会用的主流解法，时间复杂度满足题目数据范围；关键步骤加简短中文注释。
            """;

    // 用例数量未指定时的说明
    private static final String AUTO_COUNT_HINT = "按题目复杂度自行决定组数，2 到 5 组";

    // 题面草稿的用户提示
    public static String draftUser(String description) {
        return "题目描述：" + description;
    }

    // 解法示例的用户提示
    public static String solutionUser(String title, String content, String defaultCode) {
        return "【标题】" + title + "\n\n【题目描述】\n" + content + "\n\n【方法签名】\n" + defaultCode;
    }

    // 用例输入的用户提示
    public static String caseUser(String title, String content, String defaultCode, String mainFunc,
                                  Integer count, String existingInputs) {
        String countText = count == null ? AUTO_COUNT_HINT : count + " 组";
        return "请生成测试用例输入（" + countText + "）。\n\n"
                + "【标题】" + title + "\n\n"
                + "【题目描述】\n" + content + "\n\n"
                + "【方法签名】\n" + defaultCode + "\n\n"
                + "【main 函数】\n" + mainFunc + "\n\n"
                + "【已有用例的判题输入】\n" + existingInputs;
    }
}
