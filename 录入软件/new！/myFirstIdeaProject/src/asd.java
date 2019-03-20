import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.swing.*;
import java.util.*;

public class asd {
    private JPanel panel1;
    private JTextField tF_excelName;
    private JTextArea textArea1;

    List<Integer> sucessIndexs;
    List<Integer> reDuntIndexs;
    List<String> errors = new ArrayList<>();
    List<List<Object>> datalist = new ArrayList<List<Object>>();
    Thread th;
    SeleniumUtils su;
    public static void main(String[] args) {
//        JFrame frame = new JFrame("丹灶科学录入");
        JFrame frame = new JFrame("西樵科学录入");
        frame.setContentPane(new asd().panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(800,400);
        frame.setVisible(true);

    }

    public asd(){
        doButton.addActionListener(e -> {
            int i = Integer.parseInt(tF_RELOAD.getText());
            th = new Thread(() -> {
                try {
                    su.start();
                    writeExcel();
                }catch (Exception ee){
                    ee.printStackTrace();
                    //输出
                    writeExcel();

                    while(!su.isFinished){
                        System.out.println("reload");
                        try{
                            su.reload();
                            su.start();
                        }catch (Exception eeee){
                            eeee.printStackTrace();
                            //输出
                            writeExcel();
                            textArea1.setText(eeee.toString());
                            errors.add(eeee.toString());
                        }
                    }
                    //输出
                    writeExcel();
                    textArea1.append(textArea1.getText()+"\n 完成！");
                    su.wd.quit();
                    errors.add(ee.toString());
                }
            });

            try {
                sucessIndexs = new ArrayList<>();
                reDuntIndexs = new ArrayList<>();
                datalist = FileUtils.getDataList("D:\\data\\"+tF_excelName.getText()+".xls", sucessIndexs,reDuntIndexs);
                su = new SeleniumUtils(textArea1,是否蓝图CheckBox.isSelected(), datalist, sucessIndexs,reDuntIndexs,errors);
                su.setRELOAD_MAX(i);
                th.start();
                System.out.println("i:"+i);
            } catch (Exception e1) {
                writeExcel();
                textArea1.append("\n"+e1.toString());
                e1.printStackTrace();
                errors.add(e1.toString());
            }
        });

        stopButton.addActionListener(e -> {
            //su.stop();
            writeExcel();
            su.stop();
            su.wd.quit();
            su=null;
        });

        //查重模式
        button1.addActionListener(e->{
            th = new Thread(() -> {
                try {
                    su.start_quickScan();
                    writeExcel();
                }catch (Exception ee){
                    ee.printStackTrace();
                    //输出
                    writeExcel();

                    while(!su.isFinished){
                        System.out.println("reload");
                        try{
                            su.reload();
                            su.start_quickScan();
                        }catch (Exception eeee){
                            eeee.printStackTrace();
                            //输出
                            writeExcel();
                            textArea1.setText(eeee.toString());
                            errors.add(eeee.toString());
                        }
                    }
                    //输出
                    writeExcel();
                    textArea1.append(textArea1.getText()+"\n 完成！");
                    su.wd.quit();
                    errors.add(ee.toString());
                }
            });

            try {
                sucessIndexs = new ArrayList<>();
                reDuntIndexs = new ArrayList<>();
                datalist = FileUtils.getDataList("D:\\data\\"+tF_excelName.getText()+".xls", sucessIndexs,reDuntIndexs);
                su = new SeleniumUtils(textArea1,是否蓝图CheckBox.isSelected(), datalist, sucessIndexs,reDuntIndexs,errors);
                th.start();
            } catch (Exception e1) {
                writeExcel();
                textArea1.append("\n"+e1.toString());
                e1.printStackTrace();
                errors.add(e1.toString());
            }
        });

        //查重模式
        删除建筑物Button.addActionListener(e->{
            th = new Thread(() -> {
                try {
                    su.startDeleteJZW();
                    writeExcel();
                }catch (Exception ee){
                    ee.printStackTrace();
                    //输出
                    writeExcel();

                    while(!su.isFinished){
                        System.out.println("reload");
                        try{
                            su.reload();
                            su.startDeleteJZW();
                        }catch (Exception eeee){
                            eeee.printStackTrace();
                            //输出
                            writeExcel();
                            textArea1.setText(eeee.toString());
                            errors.add(eeee.toString());
                        }
                    }
                    //输出
                    writeExcel();
                    textArea1.append(textArea1.getText()+"\n 完成！");
                    su.wd.quit();
                    errors.add(ee.toString());
                }
            });

            try {
                sucessIndexs = new ArrayList<>();
                reDuntIndexs = new ArrayList<>();
                datalist = FileUtils.getDataList("D:\\data\\"+tF_excelName.getText()+".xls", sucessIndexs,reDuntIndexs);
                su = new SeleniumUtils(textArea1,是否蓝图CheckBox.isSelected(), datalist, sucessIndexs,reDuntIndexs,errors);
                th.start();
            } catch (Exception e1) {
                writeExcel();
                textArea1.append("\n"+e1.toString());
                e1.printStackTrace();
                errors.add(e1.toString());
            }
        });
    }

    public void writeExcel(){
        try {
            System.out.println("write excel");
            FileUtils.outExecl("D:\\data\\"+tF_excelName.getText()+".xls",datalist, sucessIndexs,reDuntIndexs);
        }catch (Exception e3){
            e3.printStackTrace();
            errors.add(e3.toString());
        }
    }
    private JButton doButton;
    private JCheckBox 是否蓝图CheckBox;
    private JButton stopButton;
    private JTextField tF_RELOAD;
    private JButton button1;
    private JButton 删除建筑物Button;
}
