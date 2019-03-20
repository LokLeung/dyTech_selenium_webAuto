import java.text.DecimalFormat;
import java.util.List;

public class DataBean {
    //数据字段！
    public String lon;
    public String lat;
    public String mph;
    public String lph;
    public String dyh;
    public String jlx;
    public String mplx;
    public String jzwmc;
    public String fjh;
    public String lcs;
    public String sqmc;
    public String xqmc;
    public String dwm;
    public int roomDone;

    //字段Index
    int LON_INDEX;
    int LAT_INDEX;
    int MPH_INDEX;
    int LPH_INDEX;
    int DYH_INDEX;
    int JLX_INDEX;
    int MP_TYPE;
    int JZWMC;
    int SQMC;
    int LCS;
    int FJH_INDEX;
    int XQMC;
    int DONE_INDEX;
    int DWM;

    //数据
    List<Object> data;
    int index;
    boolean isBlue;

    public DataBean(boolean isBlue, List<Object> data, int index){
        this.data = data;
        this.index = index;
        this.isBlue = isBlue;
        setIndex();
    }

    public boolean setData(){
        //读取并检查数据据！
        //已完成房间数
        String rd = data.get(DONE_INDEX).toString();
        DecimalFormat df1 = new DecimalFormat("#");
        if(rd.equals(""))
            roomDone=0;
        else{
            if(rd.contains("."))
                roomDone = Integer.parseInt(df1.format(data.get(DONE_INDEX)));
            else
                roomDone = Integer.parseInt(rd);
        }

        //坐标
        Double dlon = Double.valueOf(data.get(LON_INDEX).toString());
        Double dlat = Double.valueOf(data.get(LAT_INDEX).toString());
        DecimalFormat df = new DecimalFormat("#.000000");
        lon = df.format(dlon);
        lat = df.format(dlat);

        //门牌号
        mph = getIntData(MPH_INDEX);
        if(mph.equals(""))
            return false;
        else if(!mph.endsWith("号") && !mph.endsWith("座") && !mph.endsWith("铺"))
            mph+="号";

        //楼牌号
        lph = data.get(LPH_INDEX).toString();
        if(!lph.equals("") && !lph.endsWith("栋") && !lph.endsWith("座")&& !lph.endsWith("号"))
            lph+="栋";

        //单元号
        dyh = data.get(DYH_INDEX).toString();
        if(!dyh.equals("") && !dyh.endsWith("单元") && !dyh.endsWith("座")&& !dyh.endsWith("梯"))
            dyh+="单元";

        //房间号
        fjh = data.get(FJH_INDEX).toString();
        if(!fjh.equals("") && fjh.contains("—"))
            fjh = fjh.replace("—","-");

        //楼层数
        lcs = getIntData(LCS);
        if(lcs.equals("") && hasRoom())
            return false;

        //其他文本类型的数据
        sqmc = data.get(SQMC).toString();
        if(sqmc.equals(""))
            return false;

        jlx = data.get(JLX_INDEX).toString();
        jlx=jlx.replace(" ","");
        jlx=jlx.replace("\n","");
        jlx=jlx.replace("\r","");
        jlx=jlx.replace("\n\r","");
        if(jlx.startsWith(sqmc))
            jlx = jlx.replace(sqmc,"");
        if(jlx.equals(""))
            return false;

        mplx = data.get(MP_TYPE).toString();
        if(mplx.equals(""))
            return false;

        jzwmc = data.get(JZWMC).toString();
        xqmc = data.get(XQMC).toString();
        dwm = data.get(DWM).toString();
        //最后检查一遍数据
        return true;
    }

    public void setIndex(){
        if(isBlue){
            //是蓝图
            LON_INDEX = 2;//纵坐标
            LAT_INDEX = 3;//横坐标
            MPH_INDEX = 18;//门牌号
            LPH_INDEX = 19;//楼牌号
            DYH_INDEX = 20;//楼牌号
            JLX_INDEX = 26;//街路巷
            MP_TYPE = 25;//门牌类型
            JZWMC = 4;//建筑物名称
            FJH_INDEX = 23;//房间号
            LCS = 22;//楼层数
            SQMC = 6;//社区 or 警务室名称
            XQMC = 17;//小区名称
            DONE_INDEX = 27;//已完成房间数，放地址描述
        }else{
            LON_INDEX = 2;//纵坐标
            LAT_INDEX = 3;//横坐标
            MPH_INDEX = 7;//门牌号
            LPH_INDEX = 14;//楼牌号
            DYH_INDEX = 17;//单元号
            JLX_INDEX = 6;//街路巷
            MP_TYPE = 19;//门牌类型
            JZWMC = 13;//建筑物名称
            FJH_INDEX = 18;//房间号
            LCS = 16;//楼层数
            SQMC = 5;//社区 or 警务室名称
            XQMC = 8;//小区名称
            DONE_INDEX = 11;//已完成房间数,放到医院名称
            DWM = 15;
        }
    }

    private String getIntData(int index){
        DecimalFormat df1 = new DecimalFormat("#");
        var obj = data.get(index);
        if(obj == null)
            return "";
        String Int = data.get(index).toString();
        if(Int.contains("."))
            Int = df1.format(data.get(index));

        return Int;
    }

    public boolean isXQ(){
        return !xqmc.equals("");
    }

    public boolean hasRoom(){
        return !fjh.equals("") && fjh.contains("-");
    }

    public String getJWS(){
        return data.get(SQMC-1).toString();
    }
}
