package ApplicationForOnline.Controller;

import ApplicationForOnline.Model.LogItem;
import Utility.XMLUtility;
import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class WebLog {

    public static final String _FromTxt = "C:\\Users\\kennyfive05\\Downloads\\WEBLog.txt";
    public static final String _ToDir = "C:\\Users\\kennyfive05\\Downloads\\test\\";

    public static void main(String[] args) throws IOException, DocumentException, ParseException {
        WebLog amlLog = new WebLog();
        amlLog.run();
    }

    public void run() throws IOException, DocumentException, ParseException {
        System.out.println("--- Read File ---");
        List[] list = readFile();
        System.out.println("--- Merge ---");
        List<LogItem> items = rqrsMerge(list);
        System.out.println("--- WriteFile ---");
        writeFile(items);
        System.out.println("--- End ---");
    }

    private List[] readFile() throws IOException {
        List[] result = new List[4];

        List<String> list = FileUtils.readLines(new File(_FromTxt), StandardCharsets.UTF_8);
        List<String> eaiStr = Arrays.asList("Request Send : ", "Response received : ");
        for (int i = 0; i < 4; i++) {
            result[i] = new ArrayList();
        }

        StringBuilder sb = new StringBuilder();
        String tag = "";
        int flag = -1;
        for (String line : list) {
            if (!"".equals(sb.toString())) {
                sb.append(line);
            } else {
                for (int i = 0; i < eaiStr.size(); i++) {
                    String key = eaiStr.get(i);
                    if (line.contains(key)) {
                        flag = i;
                        sb = new StringBuilder();
                        int start = line.indexOf(key) + key.length();
                        tag = line.substring(start + 1, line.indexOf(">", start));
                        sb.append(line.substring(start));
                        break;
                    }
                }
            }

            if (!"".equals(sb.toString()) && line.contains("</" + tag + ">")) {
                result[flag].add(sb.toString());
                sb = new StringBuilder();
            }
        }

        return result;
    }

    private List<LogItem> rqrsMerge(List<String>[] list) throws DocumentException {

        List<LogItem> items = new LinkedList<>();

        Map<String, String> rsMap = new HashMap<>();

        for (String rsXML : list[1]) {
            rsMap.put(XMLUtility.getFirstValue(rsXML, "FrnMsgID"), rsXML);
        }

        for (String rqXML : list[0]) {
            LogItem item = new LogItem();
            item.setFrnMsgID(XMLUtility.getFirstValue(rqXML, "FrnMsgID"));
            item.setRqXML(rqXML);
            item.setRsXML(rsMap.get(XMLUtility.getFirstValue(rqXML, "FrnMsgID")));
            if (item.getRsXML() == null) {
                item.setSuccess(false);
                item.setDescription(String.format("no Merger, FrnMsgID: %s", item.getFrnMsgID()));
                System.out.println(item.getDescription());
            } else {
                item.setSuccess(true);
            }

            items.add(item);
        }

        return items;
    }

    private void writeFile(List<LogItem> items) throws DocumentException, ParseException, IOException {
        SimpleDateFormat soapSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat hexaSdf = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat daySdf = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat timeSdf = new SimpleDateFormat("HHmmss");

        Date date;
        for (LogItem item : items) {
            String svcType = XMLUtility.getFirstValue(item.getRqXML(), "SvcType");
            String clientID = XMLUtility.getFirstValue(item.getRqXML(), "ClientID");
            String day, id = "", dirName, rqfileName, rsfileName = "";
            if (item.getRqXML().contains("RqXMLData")) {
                // Hexa
                date = hexaSdf.parse(XMLUtility.getFirstValue(item.getRqXML(), "ClientDtTm"));
                day = daySdf.format(date);
                id = XMLUtility.getFirstValue(item.getRqXML(), "AuthData", "CustPermId");
                dirName = String.format("%s_%s_%s", day, timeSdf.format(date), svcType);
                rqfileName = String.format("%s_%s_%s.txt", svcType, XMLUtility.getFirstValue(item.getRqXML(), "SvcCode"), hexaSdf.format(date));
                if (item.getRsXML() != null) {
                    svcType = XMLUtility.getFirstValue(item.getRsXML(), "SvcType");
                    rsfileName = String.format("%s_%s_%s.txt", svcType, XMLUtility.getFirstValue(item.getRsXML(), "SvcCode"), XMLUtility.getFirstValue(item.getRsXML(), "PrcDtTm"));
                    List<String> statusCode = Arrays.asList("0000", "C080", "C081");
                    if (!statusCode.contains(XMLUtility.getFirstValue(item.getRsXML(), "Header", "StatusCode"))) {
                        item.setSuccess(false);
                        item.setDescription(String.format("StatusCode: %s, FrnMsgID: %s", XMLUtility.getFirstValue(item.getRsXML(), "Header", "StatusCode"), item.getFrnMsgID()));
                    }
                }
            } else {
                // Soap
                date = soapSdf.parse(XMLUtility.getFirstValue(item.getRqXML(), "ClientDtTm"));
                day = daySdf.format(date);
                if (item.getRqXML().contains("party_number")) {
                    id = XMLUtility.getFirstValue(item.getRqXML(), "party_number");
                } else if (item.getRqXML().contains("CustKey")) {
                    id = XMLUtility.getFirstValue(item.getRqXML(), "CustKey");
                }
                dirName = String.format("%s_%s_%s", day, timeSdf.format(date), svcType);
                rqfileName = String.format("%s_Rq_%s.txt", svcType, hexaSdf.format(date));
                if (item.getRsXML() != null) {
                    date = soapSdf.parse(XMLUtility.getFirstValue(item.getRsXML(), "ClientDtTm"));
                    svcType = XMLUtility.getFirstValue(item.getRsXML(), "SvcType");
                    rsfileName = String.format("%s_Rs_%s.txt", svcType, hexaSdf.format(date));
                }
            }

            if ("".equals(id)) {
                id = "Unknow";
                System.out.printf("id Unknow, FrnMsgID: %s%n", item.getFrnMsgID());
            }

            String dir = String.format("%s%s\\%s\\new\\%s\\%s", _ToDir, day, clientID, id, dirName);
            if (!item.isSuccess()) {
                dir += "_Fail";
            }
            dir += "\\";

            FileUtils.forceMkdir(new File(dir));
            FileUtils.write(new File(dir + rqfileName), XMLUtility.format(item.getRqXML()), StandardCharsets.UTF_8);
            if (!"".equals(rsfileName)) {
                FileUtils.write(new File(dir + rsfileName), XMLUtility.format(item.getRsXML()), StandardCharsets.UTF_8);
            }
        }
    }
}
