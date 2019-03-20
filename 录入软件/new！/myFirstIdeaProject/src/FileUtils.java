import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
/**
 * Created by Administrator on 2017/6/13.
 * 操作文件，读取execl
 */
public class FileUtils {

    public static List<List<Object>> getDataList(String path, List<Integer> sucessIndexs, List<Integer> reDuntIndexs)throws Exception{
        List<List<Object>> list = new ArrayList<List<Object>>();
        HSSFWorkbook book = new HSSFWorkbook(new FileInputStream(path));
        HSSFSheet sheet = book.getSheetAt(0);
        var colorDone = IndexedColors.LIGHT_YELLOW.getIndex();
        var colorRedunt = IndexedColors.LIGHT_BLUE.getIndex();

        System.out.println("last:"+sheet.getLastRowNum());
        for(int i=0; i<sheet.getLastRowNum()+1; i++) {
            List<Object> list1=new ArrayList<Object>();
            HSSFRow row = sheet.getRow(i);
            if(row == null)
                break;
            if(row.getCell(0)==null)
                break;
            CellStyle rowstyle = row.getCell(0).getCellStyle();
            if(rowstyle.getFillForegroundColor() == colorDone)
                sucessIndexs.add(i);
            if(rowstyle.getFillForegroundColor() == colorRedunt)
                reDuntIndexs.add(i);
            for(int j=0;j<row.getLastCellNum();j++){
                Object object =getCellValue(row.getCell(j));
                if(object==null)
                    list1.add("");
                else
                    list1.add(object);
            }
            list.add(list1);
        }
        System.out.println("共有 " + (list.size() - sucessIndexs.size() - reDuntIndexs.size()) + " 条数据：");
        return list;
    }
    //判断单元格的值
    private static  Object getCellValue(HSSFCell hssfCell){
       /* CELL_TYPE_NUMERIC 数值型 0
        CELL_TYPE_STRING 字符串型 1
        CELL_TYPE_FORMULA 公式型 2
        CELL_TYPE_BLANK 空值 3
        CELL_TYPE_BOOLEAN 布尔型 4
        CELL_TYPE_ERROR 错误 5*/
        if(hssfCell==null){
            return "";
        }
        if(hssfCell.getCellType()==CellType.NUMERIC){
            return hssfCell.getNumericCellValue();
            /*if(HSSFDateUtil.isCellDateFormatted(hssfCell)){
                return hssfCell.getDateCellValue();
            }else{
                Object o =hssfCell.getNumericCellValue();
                String str=String.valueOf((Double) o);
                String sxff=str.substring(str.indexOf(".")+1,str.length());
                Integer int_sxff=Integer.parseInt(sxff);
                //System.out.println("尾数:"+int_sxff);
                if(int_sxff==0){
                    Double d=(Double)o;
                    Integer i = d.intValue();
                    return i;
                }else{
                    return hssfCell.getNumericCellValue();
                }
            }*/
        }
        if(hssfCell.getCellType()==CellType.STRING){
            return hssfCell.getStringCellValue();
        }
        /*if(hssfCell.getCellType()==HSSFCell.CELL_TYPE_FORMULA){
            return hssfCell.getDateCellValue();
        }*/
        if(hssfCell.getCellType()==CellType.BLANK){
            return null;
        }
        if(hssfCell.getCellType()==CellType.BOOLEAN){
            return hssfCell.getBooleanCellValue();
        }
        if(hssfCell.getCellType()==CellType.ERROR){
            return null;
        }
        return null;
    }

    public static void outExecl(String path,List<List<Object>> list, List<Integer> sucessIndexs, List<Integer> reDuntIndexs)throws Exception{
        System.out.println(list.size());
        HSSFWorkbook workbook=new HSSFWorkbook();
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());//填红色
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND); //填充单元格
        CellStyle cellStyleRedunt = workbook.createCellStyle();
        cellStyleRedunt.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());//填红色
        cellStyleRedunt.setFillPattern(FillPatternType.SOLID_FOREGROUND); //填充单元格

        HSSFSheet sheet=workbook.createSheet("sheet0");
        for(int i=0;i<list.size();i++){
            HSSFRow row=sheet.createRow(i);
            List<Object> objectList= list.get(i);
            if(sucessIndexs.contains(i)){//完成的填黄色
                for(int j=0;j<objectList.size();j++){
                    HSSFCell cell=row.createCell(j);
                    cell.setCellStyle(cellStyle);
                    Object obj=objectList.get(j);
                    if(obj==null){
                        continue;
                    }
                    if("java.lang.Double".equals(objectList.get(j).getClass().getName())){
                        Double value=(Double)obj;
                        cell.setCellValue(value);
                    }else{
                        cell.setCellValue(obj.toString());
                    }
                }
            }else if(reDuntIndexs.contains(i)){//重复的蓝色
                for(int j=0;j<objectList.size();j++){
                    HSSFCell cell=row.createCell(j);
                    cell.setCellStyle(cellStyleRedunt);
                    Object obj=objectList.get(j);
                    if(obj==null){
                        continue;
                    }
                    if("java.lang.Double".equals(objectList.get(j).getClass().getName())){
                        Double value=(Double)obj;
                        cell.setCellValue(value);
                    }else{
                        cell.setCellValue(obj.toString());
                    }
                }
            }else{//不知道发生什么的不染色
                for(int j=0;j<objectList.size();j++){
                    HSSFCell cell=row.createCell(j);
                    Object obj=objectList.get(j);
                    if(obj==null){
                        continue;
                    }
                    if("java.lang.Double".equals(objectList.get(j).getClass().getName())){
                        Double value=(Double)obj;
                        cell.setCellValue(value);
                    }else{
                        cell.setCellValue(obj.toString());
                    }
                }
            }
        }
        OutputStream outputStream=new  FileOutputStream(new File(path));
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
    }
}
