package ApplicationForOnline.Frame;

import ApplicationForOnline.Controller.Update;
import ApplicationForOnline.Model.UpdateModel;
import Utility.CommonUtility;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class UpdateFrame {

    private Update DATA;
    private int X = 0;
    private int Y = 0;
    private int WIDTH = 0;
    private int HEIGHT = 30;

    public static void main(String[] args) {
        UpdateFrame gui = new UpdateFrame();
        gui.run();
    }

    private void run() {
        UpdateModel data = getUpdate();
        jframe(data);
    }

    private UpdateModel getUpdate() {
        UpdateModel data = new UpdateModel();
        if (DATA == null) {
            DATA = new Update();
        }
        try {
            data = DATA.LoadProperties();
        } catch (IOException e) {
            CommonUtility.null2Empty(data);
        }
        return data;
    }

    private void jframe(UpdateModel data) {
        JFrame jframe = new JFrame();
        jframe.setSize(800, 500);
        jframe.setLayout(null);
        jframe.setTitle("換版工具");
        jframe.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jframe.setLocation(0, 0); // 設定視窗開啟時左上角的座標，也可帶入Point物件
        jframe.setLocationRelativeTo(null); // 設定開啟的位置和某個物件相同，帶入null則會在畫面中間開啟

        Container cp = jframe.getContentPane();

        // execl sheet
        nextRow(10, 10);
        JLabel n11 = new JLabel("Sheet", JLabel.RIGHT);
        n11.setBounds(nextX(), Y, setWIDTH(100), HEIGHT);
        cp.add(n11);

        JTextField n12 = new JTextField(data.getSheet());
        n12.setBounds(nextX(), Y, setWIDTH(400), HEIGHT);
        cp.add(n12);

        // 換版清單 execl
        nextRow();
        JLabel n21 = new JLabel("換版清單(execl)", JLabel.RIGHT);
        n21.setBounds(nextX(), Y, setWIDTH(100), HEIGHT);
        cp.add(n21);

        JTextField n22 = new JTextField(data.getFromExecl());
        n22.setBounds(nextX(), Y, setWIDTH(400), HEIGHT);
        cp.add(n22);

        JButton n23 = new JButton("選擇檔案");
        n23.setBounds(nextX(), Y, setWIDTH(100), HEIGHT);
        cp.add(n23);

        n23.addActionListener(ae -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new java.io.File(n22.getText()));
            fileChooser.setDialogTitle("換版清單(execl)");
            //判斷是否選擇檔案
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();//指派給File
                n22.setText(selectedFile.getPath());
            }
        });

        // 來源資料夾
        nextRow();
        JLabel n31 = new JLabel("來源資料夾", JLabel.RIGHT);
        n31.setBounds(nextX(), Y, setWIDTH(100), HEIGHT);
        cp.add(n31);

        JTextField n32 = new JTextField(data.getFromPath());
        n32.setBounds(nextX(), Y, setWIDTH(400), HEIGHT);
        cp.add(n32);

        JButton n33 = new JButton("選擇資料夾");
        n33.setBounds(nextX(), Y, setWIDTH(100), HEIGHT);
        cp.add(n33);

        n33.addActionListener(ae -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new java.io.File(n32.getText()));
            fileChooser.setDialogTitle("來源資料夾");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                n32.setText(fileChooser.getSelectedFile().toString());
            }
        });

        // 目的資料夾
        nextRow();
        JLabel n41 = new JLabel("目的資料夾", JLabel.RIGHT);
        n41.setBounds(nextX(), Y, setWIDTH(100), HEIGHT);
        cp.add(n41);

        JTextField n42 = new JTextField(data.getToPath());
        n42.setBounds(nextX(), Y, setWIDTH(400), HEIGHT);
        cp.add(n42);

        JButton n43 = new JButton("選擇資料夾");
        n43.setBounds(nextX(), Y, setWIDTH(100), HEIGHT);
        cp.add(n43);

        n43.addActionListener(ae -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new java.io.File(n42.getText()));
            fileChooser.setDialogTitle("目的資料夾");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                n42.setText(fileChooser.getSelectedFile().toString());
            }
        });

        // PROD發行資料夾
        nextRow();
        JLabel n51 = new JLabel("發行資料夾", JLabel.RIGHT);
        n51.setBounds(nextX(), Y, setWIDTH(100), HEIGHT);
        cp.add(n51);

        JTextField n52 = new JTextField(data.getPublishPath());
        n52.setBounds(nextX(), Y, setWIDTH(400), HEIGHT);
        cp.add(n52);

        JButton n53 = new JButton("選擇資料夾");
        n53.setBounds(nextX(), Y, setWIDTH(100), HEIGHT);
        cp.add(n53);

        n53.addActionListener(ae -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new java.io.File(n52.getText()));
            fileChooser.setDialogTitle("PROD發行資料夾");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                n52.setText(fileChooser.getSelectedFile().toString());
            }
        });

        // Copy File
        nextRow();
        JLabel n61 = new JLabel("是否要複製檔案", JLabel.RIGHT);
        n61.setBounds(nextX(), Y, setWIDTH(100), HEIGHT);
        cp.add(n61);

        JCheckBox n62 = new JCheckBox();
        n62.setSelected(data.isCopy());
        n62.setBounds(nextX(), Y, setWIDTH(30), HEIGHT);
        cp.add(n62);

        // 分隔線
        nextRow();
        JLabel m00 = new JLabel("-----------------------------------------------------------------------------------------------------------------------------------------------------------");
        m00.setBounds(nextX(), Y, setWIDTH(650), 10);
        cp.add(m00);

        // textArea
        nextRow(10, Y + 30);
        JLabel m01 = new JLabel("訊息說明", JLabel.RIGHT);
        m01.setBounds(nextX(), Y, setWIDTH(100), HEIGHT);
        cp.add(m01);

        JTextArea m02 = new JTextArea("Ready...\r\n");
//        m02.setEditable(false); //禁止輸入
        m02.setLineWrap(true); //自動換行
        m02.setWrapStyleWord(true); //斷行不斷字
        JScrollPane sbrText = new JScrollPane(m02);
        sbrText.setBounds(nextX(), Y, setWIDTH(500), 150);
        cp.add(sbrText);

        // 功能鍵
        nextRow(650, 10);
        JButton f01 = new JButton("執行");
        f01.setBounds(nextX(), Y, setWIDTH(100), 50);
        cp.add(f01);

        f01.addActionListener(ae -> {
            try {
                data.setSheet(n12.getText());
                data.setCopy(n62.isSelected());
                data.setFromExecl(n22.getText());
                data.setFromPath(n32.getText());
                data.setToPath(n42.getText());
                data.setPublishPath(n52.getText());
                m02.append(DATA.run(data) + "\r\n");
            } catch (Exception e) {
                e.printStackTrace();
                m02.append(e.getMessage() + "\r\n");
            }
        });

        nextRow(X, Y + 60);
        JButton f02 = new JButton("保存");
        f02.setBounds(nextX(), Y, setWIDTH(100), 50);
        cp.add(f02);

        f02.addActionListener(ae -> {
            data.setSheet(n12.getText());
            data.setCopy(n62.isSelected());
            data.setFromExecl(n22.getText());
            data.setFromPath(n32.getText());
            data.setToPath(n42.getText());
            data.setPublishPath(n52.getText());
            m02.append(DATA.saveProperties(data) + "\r\n");
        });

        // 必須在最後，不然畫面啟始會顯示不完全
        jframe.setVisible(true);
    }

    private void nextRow() {
        nextRow(10, Y + HEIGHT + 10);
    }

    private void nextRow(int x, int y) {
        X = x - 10;
        Y = y;
        WIDTH = 0;
    }

    private int nextX() {
        return nextX(10);
    }

    private int nextX(int x) {
        return X = X + WIDTH + x;
    }

    private int setWIDTH(int width) {
        return WIDTH = width;
    }
}
