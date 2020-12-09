import org.teavm.jso.JSBody;

import java.util.ArrayList;
import java.util.HashMap;

public class BlockTool {
    int blockCount = 0;
    private static boolean addXml = true;
    private final ArrayList<String> variables = new ArrayList<>();
    private final StringBuilder functions = new StringBuilder();
    private static boolean parentIsExecuteValue = false;
    private final HashMap<String, ArrayList<String>> functionParameters = new HashMap<>();

    @JSBody(params = { "functionName" }, script = "return functions[functionName] || null")
    public static native String getFunctionBlock(String functionName);

    private String putVals(ValueBase val) {
        if (val instanceof SyntaxTree.Number) {
            return "<block type=\"math_number\"><field name=\"NUM\">" + val + "</field></block>";
        } else if (val instanceof SyntaxTree.Text) {
            return "<block type=\"text\"><field name=\"TEXT\">" + val + "</field></block>";
        } else if (val instanceof SyntaxTree.Add) {
            return "<block type=\"math_arithmetic\"><field name=\"OP\">ADD</field><value name=\"A\">" +
                    putVals(((SyntaxTree.Add) val).getV1()) + "</value><value name=\"B\">" +
                    putVals(((SyntaxTree.Add) val).getV2()) + "</value></block>";
        } else if (val instanceof SyntaxTree.Sub) {
            return "<block type=\"math_arithmetic\"><field name=\"OP\">MINUS</field><value name=\"A\">" +
                    putVals(((SyntaxTree.Sub) val).getV1()) + "</value><value name=\"B\">" +
                    putVals(((SyntaxTree.Sub) val).getV2()) + "</value></block>";
        } else if (val instanceof SyntaxTree.Mul) {
            return "<block type=\"math_arithmetic\"><field name=\"OP\">MULTIPLY</field><value name=\"A\">" +
                    putVals(((SyntaxTree.Mul) val).getV1()) + "</value><value name=\"B\">" +
                    putVals(((SyntaxTree.Mul) val).getV2()) + "</value></block>";
        } else if (val instanceof SyntaxTree.Div) {
            return "<block type=\"math_arithmetic\"><field name=\"OP\">DIVIDE</field><value name=\"A\">" +
                    putVals(((SyntaxTree.Div) val).getV1()) + "</value><value name=\"B\">" +
                    putVals(((SyntaxTree.Div) val).getV2()) + "</value></block>";
        } else if (val instanceof SyntaxTree.Mod) {
            return "<block type=\"math_modulo\"><value name=\"DIVIDEND\">" +
                    putVals(((SyntaxTree.Mod) val).getV1()) + "</value><value name=\"DIVISOR\">" +
                    putVals(((SyntaxTree.Mod) val).getV2()) + "</value></block>";
        } else if (val instanceof SyntaxTree.Pow) {
            return "<block type=\"math_arithmetic\"><field name=\"OP\">POWER</field><value name=\"A\">" +
                    putVals(((SyntaxTree.Pow) val).getV1()) + "</value><value name=\"B\">" +
                    putVals(((SyntaxTree.Pow) val).getV2()) + "</value></block>";
        } else if (val instanceof SyntaxTree.Variable) {
            String[] varName = ((SyntaxTree.Variable) val).getVariableName().split(":");
            return "<block type=\"variables_get\"><field name=\"VAR\">" + varName[varName.length - 1] + "</field></block>";
        } else if (val instanceof SyntaxTree.Equals) {
            if (((SyntaxTree.Equals) val).getV1() instanceof SyntaxTree.Text && ((SyntaxTree.Equals) val).getV1().toString().equals("") &&
                    !(((SyntaxTree.Equals) val).getV2() instanceof SyntaxTree.Number || ((SyntaxTree.Equals) val).getV2() instanceof SyntaxTree.Boolean)) {
                return "<block type=\"text_isEmpty\"><value name=\"VALUE\">" +
                        putVals(((SyntaxTree.Equals) val).getV2()) + "</value></block>";
            } else if (((SyntaxTree.Equals) val).getV2() instanceof SyntaxTree.Text && ((SyntaxTree.Equals) val).getV2().toString().equals("") &&
                    !(((SyntaxTree.Equals) val).getV1() instanceof SyntaxTree.Number || ((SyntaxTree.Equals) val).getV1() instanceof SyntaxTree.Boolean)) {
                return "<block type=\"text_isEmpty\"><value name=\"VALUE\">" +
                        putVals(((SyntaxTree.Equals) val).getV1()) + "</value></block>";
            }
            return "";
        } else if (val instanceof SyntaxTree.CallFunction) {
            String functionName = ((SyntaxTree.CallFunction) val).getFunctionName();
            ((SyntaxTree.CallFunction) val).findFunction();
            StringBuilder tmp = new StringBuilder("<block type=\"");
            if (getFunctionBlock(((SyntaxTree.CallFunction) val).getFunctionName()) == null) {
                if (parentIsExecuteValue)
                    tmp.append("procedures_callnoreturn");
                else
                    tmp.append("procedures_callreturn");
                tmp.append("\"><mutation name=\"").append(functionName).append("\">");
                StringBuilder args = new StringBuilder();
                int counter = 0;
                System.out.println(functionName);
                for (String string : functionParameters.keySet()) {
                    System.out.println(string);
                }
                for (String i : functionParameters.get(functionName)) {
                    System.out.println(i);
                    args.append("<value name=\"ARG").append(counter).append("\">")
                            .append(putVals(((SyntaxTree.SetVariable)((SyntaxTree.CallFunction) val).getVariableSetters()[counter]).getVariableValue()))
                            .append("</value>");
                    tmp.append("<arg name=\"").append(i).append("\"></arg>");
                    counter++;
                }
                tmp.append("</mutation>").append(args).append("</block>");
            } else {
                tmp.append(getFunctionBlock(((SyntaxTree.CallFunction) val).getFunctionName())).append("\">");
                int counter = 0;
                for (ProgramBase value : ((SyntaxTree.CallFunction) val).getVariableSetters()) {
                    if (((SyntaxTree.SetVariable) value).getVariableValue() instanceof SyntaxTree.Lambda) {
                        addXml = false;
                        int pBlockCount = blockCount;
                        tmp.append("<statement name=\"ARG").append(counter++).append("\">")
                                .append(syntaxTreeToBlocksXML(((SyntaxTree.Lambda) ((SyntaxTree.SetVariable) value).getVariableValue()).getCreateLambda().getProgram()))
                                .append("</statement>");
                        addXml = true;
                        blockCount = pBlockCount;
                    } else {
                        tmp.append("<value name=\"ARG").append(counter++).append("\">")
                                .append(putVals(((SyntaxTree.SetVariable) value).getVariableValue())).append("</value>");
                    }
                }
                blockCount++;
            }
            return tmp.toString();
        }
        return "";
    }

    public String syntaxTreeToBlocksXML(ProgramBase program) {
        StringBuilder tmp = new StringBuilder((addXml? "<xml xmlns=\"https://developers.google.com/blockly/xml\">" : ""));
        String xml = syntaxTreeToBlocksXML1(program);
        if (addXml) tmp.append(functions.toString());
        tmp.append(xml);
        blockCount--;
        for (int i = 0; i < blockCount; i++) {
            tmp.append("</block></next>");
        }
        if (blockCount >= 0) tmp.append("</block>");
        tmp.append(addXml? "</xml>":"");
        System.out.println(tmp);
        return tmp.toString();
    }

    public String syntaxTreeToBlocksXML1(ProgramBase program) {
        StringBuilder result;
        if (blockCount != 0 && !(program instanceof SyntaxTree.Function)) {
            result = new StringBuilder("<next>");
        } else {
            result = new StringBuilder();
        }
        if (program instanceof SyntaxTree.Programs) {
            for (ProgramBase program1 : ((SyntaxTree.Programs) program).getPrograms()) {
                result.append(syntaxTreeToBlocksXML1(program1));
            }
        } else if (program instanceof SyntaxTree.Print) {
            ValueBase[] args = ((SyntaxTree.Print) program).getArgs();
            for (int i = 0; i < args.length; i++) {
                result.append("<block type=\"text_print\">" + "<value name=\"TEXT\">").append(putVals(args[i]))
                        .append("</value>");
                blockCount++;
                if (i < args.length - 1) {
                    result.append("<next><block type=\"text_print\">" + "<value name=\"TEXT\">").append(putVals(((SyntaxTree.Print) program).getSeparator()))
                            .append("</value>");
                    blockCount++;
                }
            }
        } else if (program instanceof SyntaxTree.SetVariable) {
            if (variables.contains(((SyntaxTree.SetVariable) program).getVariableName())) {
                if (((SyntaxTree.SetVariable) program).getVariableValue() instanceof SyntaxTree.Add) {
                    result.append("<block type=\"math_change\"><field name=\"VAR\">").append(((SyntaxTree.SetVariable) program).getVariableName())
                            .append("</field><value name=\"DELTA\">").append(putVals(((SyntaxTree.Add) ((SyntaxTree.SetVariable) program).getVariableValue()).getV2()))
                            .append("</value>");
                } else {
                    result.append("<block type=\"variables_set\"><field name=\"VAR\">").append(((SyntaxTree.SetVariable) program).getVariableName())
                            .append("</field><value name=\"VALUE\">").append(putVals(((SyntaxTree.SetVariable) program).getVariableValue()))
                            .append("</value>");
                }
                blockCount++;
            } else {
                variables.add(((SyntaxTree.SetVariable) program).getVariableName());
                if (!(((SyntaxTree.SetVariable) program).getVariableValue() instanceof SyntaxTree.Null)) {
                    if (((SyntaxTree.SetVariable) program).getVariableValue() instanceof SyntaxTree.Add) {
                        result.append("<block type=\"math_change\"><field name=\"VAR\">").append(((SyntaxTree.SetVariable) program).getVariableName())
                                .append("</field><value name=\"DELTA\">").append(putVals(((SyntaxTree.Add) ((SyntaxTree.SetVariable) program).getVariableValue()).getV2()))
                                .append("</value>");
                    } else {
                        result.append("<block type=\"variables_set\"><field name=\"VAR\">").append(((SyntaxTree.SetVariable) program).getVariableName())
                                .append("</field><value name=\"VALUE\">").append(putVals(((SyntaxTree.SetVariable) program).getVariableValue()))
                                .append("</value>");
                    }
                    blockCount++;
                }
            }
        } else if (program instanceof SyntaxTree.ExecuteValue) {
            parentIsExecuteValue = true;
            result.append(putVals(((SyntaxTree.ExecuteValue) program).getValue()));
            parentIsExecuteValue = false;
        } else if (program instanceof SyntaxTree.Function) {
            if (getFunctionBlock((((SyntaxTree.Function) program).getFunctionName())) == null) {
                boolean hasReturn = hasReturn(((SyntaxTree.Function) program).getProgram());
                if (!functionParameters.containsKey(((SyntaxTree.Function) program).getFunctionName())) {
                    functionParameters.put(((SyntaxTree.Function) program).getFunctionName().split(":")[0], new ArrayList<>());
                }
                if (hasReturn) {
                    functions.append("<block type=\"procedures_defreturn\"><mutation>");
                    for (String i : ((SyntaxTree.Function) program).getArgs()) {
                        functionParameters.get(((SyntaxTree.Function) program).getFunctionName().split(":")[0]).add(i);
                        functions.append("<arg name=\"").append(i).append("\"></arg>");
                    }
                    addXml = false;
                    int pBlockCount = blockCount;
                    functions.append("</mutation>").append("<field name=\"NAME\">").append(((SyntaxTree.Function) program)
                            .getFunctionName().split(":")[0]).append("</field><statement name=\"STACK\">")
                            .append(syntaxTreeToBlocksXML(((SyntaxTree.Function) program).getProgram())).append("</statement></block>");
                    addXml = true;
                    blockCount = pBlockCount;
                } else {
                    functions.append("<block type=\"procedures_defnoreturn\"><mutation>");
                    for (String i : ((SyntaxTree.Function) program).getArgs()) {
                        functionParameters.get(((SyntaxTree.Function) program).getFunctionName().split(":")[0]).add(i);
                        functions.append("<arg name=\"").append(i).append("\"></arg>");
                    }
                    addXml = false;
                    int pBlockCount = blockCount;
                    functions.append("</mutation><field name=\"NAME\">").append(((SyntaxTree.Function) program).getFunctionName().split(":")[0])
                            .append("</field><statement name=\"STACK\">").append(syntaxTreeToBlocksXML(((SyntaxTree.Function) program).getProgram()))
                            .append("</statement></block>");
                    addXml = true;
                    blockCount = pBlockCount;
                }
            }
        }
        return result.toString();
    }

    private boolean hasReturn(ProgramBase program) {
        boolean hasReturn = false;
        if (program instanceof SyntaxTree.Programs) {
            for (ProgramBase program1 : ((SyntaxTree.Programs) program).getPrograms()) {
                hasReturn |= hasReturn(program1);
            }
        } else return program instanceof SyntaxTree.Return;
        return hasReturn;
    }
}