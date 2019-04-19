package ApplicationForOnline.Controller;

import ApplicationForOnline.Model.ProgramModel;
import ApplicationForOnline.Model.ReasonForChangeModel;
import ApplicationForOnline.Model.UpdateModel;
import Model.FileVo;
import Utility.CommonUtility;
import Utility.NumberUtility;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Update {
    private final String ERROR = "ERROR";
    private String _PropertiesPath;

    public static void main(String[] args) throws Exception {
        Update update = new Update();
        update.run(update.LoadProperties());
    }

    /**
     * 主流程
     *
     * @param data
     * @throws Exception
     */
    public String run(UpdateModel data) throws Exception {
        data = init(data);

        // 取得所有程式清單
        System.out.println("-- getProgramModel --");
        Map<String, FileVo> fileMap = getProgramModel(data.getFromPath());

        // 取得過版清單 Execl 各項目
        System.out.println("-- getReasonForChange --");
        List<ReasonForChangeModel> fileList = getReasonForChange(data, fileMap);

        // Copy Source
        if (data.isCopy()) {
            System.out.println("-- copyFile --");
            fileList = copyFile(data, fileList);
        }

        // 產生上線申請書 Execl
        System.out.println("-- CreateExecl --");
        String execl = CreateExecl(fileList, data.getToPath(), data.getPublishPath());
        System.out.println(execl);

        // log.md
        System.out.println("-- CreateErrorMessage --");
        String error = CreateErrorMessage(fileList, data.getToPath());
        System.out.println(error);

        System.out.println("-- END --");

        return error;
    }

    /**
     * load properties
     *
     * @return
     * @throws IOException
     */
    public UpdateModel LoadProperties() throws IOException {
        UpdateModel data = new UpdateModel();
        Properties properties = new Properties();
        File file = new File("Update.properties");
        if (file.isFile()) {
            _PropertiesPath = file.getAbsolutePath();
        } else {
            _PropertiesPath = Objects.requireNonNull(this.getClass().getClassLoader().getResource("Update.properties")).getPath();
        }
        properties.load(new InputStreamReader(new FileInputStream(_PropertiesPath), "utf-8"));

        //sheet
        data.setSheet(properties.getProperty("sheet"));
        data.setFromExecl(properties.getProperty("fromExecl").replace('/', '\\'));
        data.setFromPath(addDirEnd(properties.getProperty("fromPath").replace('/', '\\')));
        data.setToPath(addDirEnd(properties.getProperty("toPath").replace('/', '\\')));
        data.setPublishPath(addDirEnd(properties.getProperty("publishPath").replace('/', '\\')));
        data.setCopy(Boolean.valueOf(properties.getProperty("isCopy")));

        return data;
    }

    /**
     * save properties
     *
     * @return
     */
    public String saveProperties(UpdateModel data) {
        try {
            Properties properties = new Properties();
            properties.setProperty("sheet", data.getSheet());
            properties.setProperty("fromExecl", data.getFromExecl());
            properties.setProperty("fromPath", data.getFromPath());
            properties.setProperty("toPath", data.getToPath());
            properties.setProperty("publishPath", data.getPublishPath());
            properties.setProperty("isCopy", String.valueOf(data.isCopy()));
            OutputStream outStrem = new FileOutputStream(_PropertiesPath);
            properties.store(outStrem, "");
            return String.format("Properties 儲存成功: %s", _PropertiesPath);
        } catch (IOException ex) {
            ex.printStackTrace();
            return String.format("Properties 儲存失敗: %s\r\n%s", _PropertiesPath, ex.getMessage());
        }
    }

    /**
     * 初始化
     *
     * @param data
     * @return
     */
    private UpdateModel init(UpdateModel data) {
        data.setToPath(addVersion(data));
        data.setFromPath(addDirEnd(data.getFromPath()));
        data.setPublishPath(addDirEnd(data.getPublishPath()));

        return data;
    }

    /**
     * 檢查目錄結尾是否為'\\'
     *
     * @param path
     * @return
     */
    private String addDirEnd(String path) {
        if (path.lastIndexOf('\\') != path.length() - 1) {
            path = path + "\\";
        }
        return path;
    }

    /**
     * 產生版號
     *
     * @param data
     * @return
     */
    private String addVersion(UpdateModel data) {
        // 版次
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String path = addDirEnd(data.getToPath());
        path = String.format("%s%s_v", path, sdf.format(Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).getTime()));

        int version = NumberUtils.toInt(data.getVersion(), 1);
        path += version + "\\";
        try {
            FileUtils.deleteDirectory(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return path;
    }

    /**
     * 新增 row 的各欄位
     *
     * @param workbook
     * @param row
     * @param array
     */
    private void addHeadRow(Workbook workbook, Row row, String... array) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index); // 背景
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN); // 下邊框
        style.setBorderTop(BorderStyle.THIN); // 上邊框
        style.setBorderLeft(BorderStyle.THIN); // 左邊框
        style.setBorderRight(BorderStyle.THIN); // 右邊框
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        for (int i = 0; i < array.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(style);
            cell.setCellValue(array[i]);
        }
    }

    /**
     * 新增 row 的各欄位
     *
     * @param row
     * @param array
     */
    private void addRow(Workbook workbook, Row row, String... array) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN); // 下邊框
        style.setBorderTop(BorderStyle.THIN); // 上邊框
        style.setBorderLeft(BorderStyle.THIN); // 左邊框
        style.setBorderRight(BorderStyle.THIN); // 右邊框
        for (int i = 0; i < array.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(style);
            cell.setCellValue(array[i]);
        }
    }

    /**
     * 取得所有程式清單
     *
     * @param path
     * @return
     */
    private Map<String, FileVo> getProgramModel(String path) {
        Map<String, FileVo> fileMap = new TreeMap<>();

        Collection<File> list = FileUtils.listFiles(new File(path),
                new AbstractFileFilter() {
                    @Override
                    public boolean accept(File file) {
                        boolean result = true;
                        List<String> array = Arrays.asList("Debug\\", ".vs\\", "bin\\", "Release\\",
                                ".vspscc", ".user", "Thumbs.db",
                                "LoginEAI\\", "Web.Debug.config", "Web.Release.config",
                                "\\Models\\Pershing.EBot.Models.Communication\\說明.txt",
                                "\\Models\\Pershing.EBot.Models.Global\\ClassDiagram1.cd");
                        for (String str : array) {
                            if ((file.getPath() + file.getName()).contains(str)) {
                                result = false;
                                break;
                            }
                        }
                        return result;
                    }
                }, TrueFileFilter.INSTANCE);

        list.forEach(file -> {
            FileVo vo = new FileVo();
            vo.setPath((file.getParent() + "\\").replace(path, "\\"));
            vo.setName(file.getName());
            fileMap.put(vo.getPath() + vo.getName(), vo);
        });

        return fileMap;
    }

//    IOFileFilter fileFilter = new AbstractFileFilter() {
//        @Override
//        public boolean accept(File file) {
//            boolean result = true;
//            List<String> array = Arrays.asList("Debug\\", ".vs\\", "bin\\", "Release\\",
//                    ".vspscc", ".user", "Thumbs.db",
//                    "LoginEAI\\", "Web.Debug.config", "Web.Release.config",
//                    "\\Models\\Pershing.EBot.Models.Communication\\說明.txt",
//                    "\\Models\\Pershing.EBot.Models.Global\\ClassDiagram1.cd");
//            for (String str : array) {
//                if ((file.getPath() + file.getName()).contains(str)) {
//                    result = false;
//                    break;
//                }
//            }
//            return result;
//        }
//    };

    /**
     * 取得過版清單 Execl 各項目
     *
     * @param data
     * @param fileMap
     * @return
     * @throws IOException
     */
    private List<ReasonForChangeModel> getReasonForChange(UpdateModel data, Map<String, FileVo> fileMap) throws IOException {
        List<ReasonForChangeModel> list = new LinkedList<>();
        Workbook workbook = getWorkBook(data);
        Sheet sheet = workbook.getSheetAt(workbook.getSheetIndex(data.getSheet()));

        // 取得各標題對應的欄位 index
        Row row = sheet.getRow(0);
        List<String> cells = new LinkedList<>();
        for (int j = 0; j < row.getLastCellNum(); j++) {
            if (null != row.getCell(j) && CellType.STRING == row.getCell(j).getCellType()) {
                cells.add(row.getCell(j).toString());
            } else {
                cells.add("");
            }
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i);

            if (!checkCellValue(row, cells, "序號", "通報單", "項目", "程式清單") ||
                    "X".equalsIgnoreCase(getCell(row, cells, "UAT待換版"))) {
                continue;
            }

            // 資料
            ReasonForChangeModel model = new ReasonForChangeModel();
            try {
                model.setId(new BigDecimal(getCell(row, cells, "序號")).intValue());
            } catch (Exception ex) {
                continue;
            }
            model.setNumber(getCell(row, cells, "通報單"));
            model.setReason(getCell(row, cells, "項目"));
            model.setOnlineNumber(getCell(row, cells, "線上問題單號"));
            model.setUatNumber(getCell(row, cells, "UAT單號"));
            model.setSPrograms(getCell(row, cells, "程式清單"));

            // 程式清單fileMap
            List<ProgramModel> programs = new LinkedList<>();
            String[] array = model.getSPrograms().replace("/", "\\").split("\n");
            for (String str : array) {
                // 空白行
                if (StringUtils.isBlank(str))
                    continue;

                str = str.replace(' ', '\t');
                String[] temp = str.split("\t");
                temp = Arrays.stream(temp).filter(s -> !"".equals(s)).toArray(String[]::new);

                ProgramModel programModel = new ProgramModel();
                programModel.setExeclNo(model.getId());
                if (temp.length != 2) {
                    programModel.setName(str);
                    programModel.setStatus(ERROR);
                    programModel.setErrorMessage("getReasonForChange error: 無法解析的交易路徑！");
                } else {
                    int beginIndex = temp[0].indexOf("\\", 2);
                    while (fileMap.get(temp[0]) == null && beginIndex > 0) {
                        temp[0] = temp[0].substring(beginIndex);
                        beginIndex = temp[0].indexOf("\\", 1);
                    }
                    if (fileMap.get(temp[0]) == null) {
                        programModel.setName(str);
                        programModel.setStatus(ERROR);
                        programModel.setErrorMessage("getReasonForChange error: 無法找到對應的程式, 請確認交易路徑或名稱是否正確 or 更新專案至最新版!");
                    } else {
                        FileVo vo = fileMap.get(temp[0]);
                        programModel.setPath(vo.getPath());
                        programModel.setName(vo.getName());
                        programModel.setStatus(temp[1]);

                        // 如果有加入、刪除檔案，一定要調整 .csproj
                        if ("加入".equals(programModel.getStatus()) || "刪除".equals(programModel.getStatus())) {
                            programs.add(programModel);
                            programModel = new ProgramModel();
                            programModel.setExeclNo(model.getId());
                            programModel.setStatus("編輯");
                            if (vo.getPath().contains("Pershing.EBot.Utility")) {
                                programModel.setPath("\\Library\\Pershing.EBot.Utility\\");
                                programModel.setName("Pershing.EBot.Utility.csproj");
                            } else if (vo.getPath().contains("Pershing.EBot.Models.Communication")) {
                                programModel.setPath("\\Models\\Pershing.EBot.Models.Communication\\");
                                programModel.setName("Pershing.EBot.Models.Communication.csproj");
                            } else if (vo.getPath().contains("Pershing.EBot.Models.Global")) {
                                programModel.setPath("\\Models\\Pershing.EBot.Models.Global\\");
                                programModel.setName("Pershing.EBot.Models.Global.csproj");
                            } else {
                                programModel.setPath("\\Pershing.EBot.Project\\Pershing.EBot.Project\\");
                                programModel.setName("Pershing.EBot.Project.csproj");
                            }
                        }
                    }
                }
                programs.add(programModel);
            }
            model.setPrograms(programs);
            list.add(model);
        }

        return list;
    }

    /**
     * 依檔案取得 xls 或 xlsx
     *
     * @param data
     * @return
     * @throws IOException
     */
    private Workbook getWorkBook(UpdateModel data) throws IOException {
        String execl = data.getFromExecl();

        //創建Workbook工作薄對象，表示整個excel
        Workbook workbook;
        File file = new File(execl);

        //獲得文檔名
        String fileName = file.getName().toLowerCase();

        //獲取excel文檔的io流
        InputStream is = new FileInputStream(file);
        //根據文檔後綴名不同(xls和xlsx)獲得不同的Workbook實現類對象
        if (fileName.endsWith(".xls")) {
            workbook = new HSSFWorkbook(is);
        } else if (fileName.endsWith(".xlsx")) {
            workbook = new XSSFWorkbook(is);
        } else {
            throw new IOException(fileName + "不是excel文檔");
        }

        String toName = data.getToPath() + execl.substring(execl.lastIndexOf("\\") + 1);
        FileUtils.copyFile(new File(execl), new File(toName));
        System.out.println("execl 已複制: " + toName);

        return workbook;
    }

    /**
     * Copy Source
     *
     * @param data
     * @param fileList
     * @return
     */
    private List<ReasonForChangeModel> copyFile(UpdateModel data, List<ReasonForChangeModel> fileList) {
        String toPath = data.getToPath();
        toPath = toPath + toPath.substring(toPath.lastIndexOf('\\', toPath.length() - 2) + 1) + "\\SoruceCode\\";
        try {
            FileUtils.forceMkdir(new File(toPath.replace("\\SoruceCode\\", "\\上線申請書&測試報告\\")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (ReasonForChangeModel file : fileList) {
            for (ProgramModel programModel : file.getPrograms()) {
                String name = programModel.getName();
                if (ERROR.equals(programModel.getStatus())) {
                    continue;
                }

                String path = programModel.getPath() + name;
                path = (data.getFromPath() + path).replace("\\\\", "\\");
                try {
                    FileUtils.copyFile(new File(path), new File(path.replace(data.getFromPath(), toPath)));
                } catch (IOException e) {
                    e.printStackTrace();
                    programModel.setStatus(ERROR);
                    programModel.setErrorMessage("copyFile error: " + e.getMessage());
                }
            }
        }
        return fileList;
    }

    /**
     * 取得 cell
     *
     * @param row
     * @param cells
     * @param cellName
     * @return
     */
    private String getCell(Row row, List<String> cells, String cellName) {
        String result = "";
        int index = cells.indexOf(cellName);
        if (index > -1) {
            Cell cell = row.getCell(index);
            if (cell != null) {
                if (CellType.STRING == cell.getCellType() || CellType.NUMERIC == cell.getCellType()) {
                    result = NumberUtility.format("9", cell.toString().trim(), "ZZZ9.Z");
                }
            }
        }

        return result;
    }

    /**
     * 檢查項目是否為空值, 皆有值回傳 true
     *
     * @param row
     * @param cells
     * @param array
     * @return
     */
    private boolean checkCellValue(Row row, List<String> cells, String... array) {
        boolean flag = true;
        for (String str : array) {
            if ("".equals(getCell(row, cells, str))) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    /**
     * 產生上線申請書 Execl
     *
     * @param fileList
     * @param toPath
     * @param publishPath
     * @return
     * @throws IOException
     */
    private String CreateExecl(List<ReasonForChangeModel> fileList, String toPath, String publishPath) throws IOException, CloneNotSupportedException {
        Collections.copy(fileList, fileList);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("換版原因");
        for (int i = 0; i < fileList.size(); i++) {
            Row row = sheet.createRow((short) i);
            fileList.sort(Comparator.comparing(ReasonForChangeModel::getId));
            ReasonForChangeModel model = fileList.get(i);
            String[] array = {model.getId() + ".", model.getNumber(), model.getReason()};
            addRow(workbook, row, array);
        }

        sheet = workbook.createSheet("交版程式清單");
        Row row = sheet.createRow((short) 0);
        String[] array = {"序號", "目錄", "程式名稱", "異動", "項目編號"};
        addHeadRow(workbook, row, array);
        List<ProgramModel> newPrograms = new LinkedList<>();
        fileList.forEach(file -> newPrograms.addAll(file.getPrograms()));
        newPrograms.removeIf(p -> ERROR.equals(p.getStatus()));
        newPrograms.sort(Comparator.comparing(ProgramModel::getPath).thenComparing(ProgramModel::getName).thenComparing(ProgramModel::getExeclNo));
        String ids = "";
        short count = 1;
        for (int i = 0; i < newPrograms.size(); i++) {
            ProgramModel model = newPrograms.get(i);
            if ("".equals(ids)) {
                ids = String.valueOf(model.getExeclNo());
            } else if (!ids.contains(String.valueOf(model.getExeclNo()))) {
                ids = String.format("%s、%d", ids, model.getExeclNo());
            }
            /* 寫入execl
             * 1. 最後一筆
             * 2. 跟下一筆的檔案(path+name)不同
             */
            if (i == newPrograms.size() - 1 || !(model.getPath() + model.getName()).equals(newPrograms.get(i + 1).getPath() + newPrograms.get(i + 1).getName())) {
                row = sheet.createRow(count);
                array = new String[]{String.format("%d.", count++), model.getPath(), model.getName(), model.getStatus(), ids};
                addRow(workbook, row, array);
                ids = "";
            }
        }

        sheet = workbook.createSheet("更新線上程式");
        row = sheet.createRow((short) 0);
        array = new String[]{"序號", "目錄", "程式名稱", "異動", "項目編號"};
        addHeadRow(workbook, row, array);
        List<ProgramModel> list = createSheetByProd(newPrograms, publishPath);
        list.sort(Comparator.comparing(ProgramModel::getPath).thenComparing(ProgramModel::getName).thenComparing(ProgramModel::getExeclNo).thenComparing(ProgramModel::getStatus));
        count = 1;
        for (int i = 0; i < list.size(); i++) {
            ProgramModel model = list.get(i);
            if ("".equals(ids)) {
                ids = String.valueOf(model.getExeclNo());
            } else if (!ids.contains(String.valueOf(model.getExeclNo()))) {
                ids = String.format("%s、%d", ids, model.getExeclNo());
            }
            /* 寫入execl
             * 1. 最後一筆
             * 2. 跟下一筆的檔案(path+name)不同
             */
            if (i == list.size() - 1 || !(model.getPath() + model.getName()).equals(list.get(i + 1).getPath() + list.get(i + 1).getName())) {
                row = sheet.createRow(count);
                array = new String[]{String.valueOf(count++) + ".", model.getPath(), model.getName(), model.getStatus(), ids};
                addRow(workbook, row, array);
                ids = "";
            }
        }

        String toFileName = toPath + "上線申請書.xlsx";
        FileUtils.touch(new File(toFileName));
        FileOutputStream fileOut = new FileOutputStream(toFileName);
        workbook.write(fileOut);
        fileOut.close();
        workbook.close();
        return String.format("execl 已產生: %s", toFileName);
    }

    /**
     * 交版程式清單
     *
     * @param newPrograms
     * @param publishPath
     * @return
     */
    private List<ProgramModel> createSheetByProd(List<ProgramModel> newPrograms, String publishPath) throws CloneNotSupportedException {
        List<ProgramModel> list = new ArrayList<>();

        for (ProgramModel model : newPrograms) {

            // 根目錄直接 copy 的目錄
            List<String> names = Arrays.asList("Content\\", "DemoPage\\", "Scripts\\", "Views\\");
            for (String name : names) {
                if (model.getPath().contains(name)) {
                    ProgramModel newModel = (ProgramModel) model.clone();
                    newModel.setPath(model.getPath().substring(model.getPath().indexOf("\\" + name)));
                    list.add(newModel);
                }
            }

            // 根目錄檔案
            names = Arrays.asList("packages.config", "Web.config");
            for (String name : names) {
                if (model.getName().contains(name)) {
                    ProgramModel newModel = (ProgramModel) model.clone();
                    newModel.setPath("\\");
                    list.add(newModel);
                }
            }

            // DLL
            if (model.getPath().contains("DLL\\")) {
                ProgramModel newModel = (ProgramModel) model.clone();
                newModel.setPath("\\bin\\");
                list.add(newModel);
            }

            // Pershing.EBot.Utility.dll
            if (model.getPath().contains("Pershing.EBot.Utility")) {
                ProgramModel newModel = (ProgramModel) model.clone();
                newModel.setPath("\\bin\\");
                newModel.setName("Pershing.EBot.Utility.dll");
                list.add(newModel);
            }

            // Pershing.EBot.Models.Communication.dll
            if (model.getPath().contains("Pershing.EBot.Models.Communication")) {
                ProgramModel newModel = (ProgramModel) model.clone();
                newModel.setPath("\\bin\\");
                newModel.setName("Pershing.EBot.Models.Communication.dll");
                list.add(newModel);
            }

            // Pershing.EBot.Models.Global.dll
            if (model.getPath().contains("Pershing.EBot.Models.Global")) {
                ProgramModel newModel = (ProgramModel) model.clone();
                newModel.setPath("\\bin\\");
                newModel.setName("Pershing.EBot.Models.Global.dll");
                list.add(newModel);
                newModel = (ProgramModel) model.clone();
                newModel.setPath("\\bin\\en-US\\");
                newModel.setName("Pershing.EBot.Models.Global.resources.dll");
                list.add(newModel);
                newModel = (ProgramModel) model.clone();
                newModel.setPath("\\bin\\zh-CN\\");
                newModel.setName("Pershing.EBot.Models.Global.resources.dll");
                list.add(newModel);
            }

            // Pershing.EBot.Project.dll
            if (model.getPath().contains("Pershing.EBot.Project")) {
                ProgramModel newModel = (ProgramModel) model.clone();
                newModel.setPath("\\bin\\");
                newModel.setName("Pershing.EBot.Project.dll");
                list.add(newModel);
            }

            // Txn Controller
            if (model.getName().contains("Controller")) {
                ProgramModel newModel = (ProgramModel) model.clone();
                newModel.setPath("\\bin\\");
                newModel.setName(model.getName().substring(0, 6) + ".dll");
                if (new File(publishPath + "bin\\" + newModel.getName()).isFile()) {
                    list.add(newModel);
                }
            }

            // Txn Model
            if (model.getPath().contains("Pershing.EBot.Project\\Models") && !model.getPath().contains("Platform")) {
                ProgramModel newModel = (ProgramModel) model.clone();
                newModel.setPath("\\bin\\");
                newModel.setName(model.getName().substring(0, 6) + ".dll");
                if (new File(publishPath + "bin\\" + newModel.getName()).isFile()) {
                    list.add(newModel);
                }
            }

            // Txn View
            if (model.getPath().contains("Views\\")) {
                Collection<File> voList = FileUtils.listFiles(new File(publishPath + "bin\\"), new AbstractFileFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.contains(model.getName().toLowerCase());
                    }
                }, TrueFileFilter.INSTANCE);

                ProgramModel newModel = (ProgramModel) model.clone();
                newModel.setPath("\\bin\\");
                newModel.setName("EBOT.dll");
                list.add(newModel);
                for (File file : voList) {
                    try {
                        ProgramModel newModel2 = (ProgramModel) model.clone();
                        newModel2.setPath("\\bin\\");
                        newModel2.setName(file.getName());
                        list.add(newModel2);
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // BOTFTP
            names = Arrays.asList("eBillMenu.xml", "eMarketingUpdate.txt", "fBankData.xml", "fBrCode.xml", "fFundData.ini",
                    "fFundSap.ini", "fGoldData.xml", "FundSapInqRs.xml", "HexaMapTable.xml", "menu.xml",
                    "OccupationCode.xml", "ResidenceCountry.xml");
            if (names.contains(model.getName())) {
                ProgramModel newModel = (ProgramModel) model.clone();
                newModel.setPath("D:\\BOTFTP\\");
                newModel.setName(model.getName());
                list.add(newModel);
            }
        }

        return list;
    }

    /**
     * 建立 error Log
     *
     * @param fileList
     * @param toPath
     * @throws IOException
     */
    private String CreateErrorMessage(List<ReasonForChangeModel> fileList, String toPath) throws IOException {
        String logFile = toPath + "log.md";
        StringBuilder sb = new StringBuilder();
        List<ProgramModel> list = new LinkedList<>();
        fileList.forEach(file -> list.addAll(file.getPrograms().stream().filter(p -> ERROR.equals(p.getStatus())).collect(Collectors.toList())));
        list.forEach(CommonUtility::null2Empty);
        list.sort(Comparator.comparing(ProgramModel::getErrorMessage).thenComparing(ProgramModel::getExeclNo).thenComparing(ProgramModel::getPath).thenComparing(ProgramModel::getName));
        String message = "";
        for (ProgramModel model : list) {
            if (!message.equals(model.getErrorMessage())) {
                message = model.getErrorMessage();
                sb.append("\r\n").append(String.format("## %s", message)).append("\r\n");
            }
            sb.append(String.format("%d\\. %s%s\r\n", model.getExeclNo(), StringUtils.trimToEmpty(model.getPath()), model.getName())).append("\r\n");
        }
        sb.append("\r\n- - -\r\n");

        sb.append("\r\n## 通報單\r\n");
        List<ReasonForChangeModel> newfileList = new LinkedList<>();
        newfileList.addAll(fileList);
        newfileList.removeIf(file -> "變更".equals(file.getNumber()) || "修改".equals(file.getNumber()));
        newfileList.sort(Comparator.comparing(ReasonForChangeModel::getNumber).thenComparing(ReasonForChangeModel::getId));
        newfileList.forEach(file -> {
            String[] numbers = file.getNumber().split("\n");
            Arrays.stream(numbers).forEach(number -> sb.append("* ").append(number).append("\r\n"));
        });
        sb.append("\r\n## 線上問題單號\r\n");
        newfileList = new LinkedList<>();
        newfileList.addAll(fileList);
        newfileList.removeIf(file -> StringUtils.isBlank(file.getOnlineNumber()));
        newfileList.sort(Comparator.comparing(ReasonForChangeModel::getOnlineNumber).thenComparing(ReasonForChangeModel::getId));
        newfileList.forEach(file -> {
            String[] numbers = file.getOnlineNumber().split("\n");
            Arrays.stream(numbers).forEach(number -> sb.append("* ").append(number).append("\r\n"));
        });
        sb.append("\r\n## UAT單號\r\n");
        newfileList = new LinkedList<>();
        newfileList.addAll(fileList);
        newfileList.removeIf(file -> StringUtils.isBlank(file.getUatNumber()));
        newfileList.sort(Comparator.comparing(ReasonForChangeModel::getUatNumber).thenComparing(ReasonForChangeModel::getId));
        newfileList.forEach(file -> {
            String[] numbers = file.getUatNumber().split("\n");
            Arrays.stream(numbers).forEach(number -> sb.append("* ").append(number).append("\r\n"));
        });

        FileUtils.write(new File(logFile), sb.toString(), StandardCharsets.UTF_8);
        return String.format("log 已產生: %s", logFile);
    }
}
