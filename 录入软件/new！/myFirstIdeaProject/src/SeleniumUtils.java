import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SeleniumUtils {
    int currentIndex = 0;
    String USR = "186833";
    String PSW = "@djy123456";
    JTextArea output;
    Boolean isBlue;
    WebDriver wd;
    String fails;
    List<List<Object>> dataList;
    List<Integer> sucessIndexs;
    List<Integer> reduntIndex;
    List<String> errors;
    List<DataBean> datas = new ArrayList<>();
    Boolean isFinished = false;
    DataBean data;
    String govFullPrefix = "广东省佛山市南海区丹灶镇";
    String xingZhengqu = "南海区";
    WebDriverWait wait;

    public SeleniumUtils(JTextArea output, Boolean isBlue, List<List<Object>> dataList, List<Integer> sucessIndexs,List<Integer> reduntIndex, List<String> errors){
        this.output = output;
        this.isBlue = isBlue;
        fails="";
        this.dataList = dataList;
        this.sucessIndexs = sucessIndexs;
        this.reduntIndex = reduntIndex;
        this.errors = errors;

        setUpDatas();
        setUpDriver();
        firstLoad();
    }

    public void firstLoad(){
        String url = "http://68.28.34.112:8082/basic/mainservlet?actionType=GOTO_HOMEPAGE";
        wd.navigate().to(url);
        System.out.println("login url:"+wd.getCurrentUrl());
        login();

        //到达应用界面了，展开左侧菜单先
        //set up
        wd.switchTo().frame("leftFrame");
        System.out.println(wd.getTitle());
        waitForLoad(By.id("itm489C877DAF10F79CE050A8C00D0E12D5"),5);
        WebElement menu = wd.findElement(By.id("itm489C877DAF10F79CE050A8C00D0E12D5"));
        menu.click();

        System.out.println("到达应用界面,并展开左侧菜单");
    }

    public void start() throws InterruptedException {
        for(;currentIndex<datas.size();currentIndex++){
            //全局current data
            data = datas.get(currentIndex);
            if(data.isXQ() && data.lph.equals(""))
                continue;

            //单位元
            if(!data.dwm.equals("")){
                processDWY();
                continue;
            }

            //第一类，既不是小区也没房间的街路向！
            if(!data.isXQ() && !data.hasRoom()){
                processNormalPlate();
            }else if(!data.isXQ() && data.hasRoom()){
                //第二类，出租房，有房间但不是小区
                processChuzhuFang();
            }else if(data.isXQ()){
                processXQ();
            }
            output.setText("最后处理的一条："+data.index);
        }
        isFinished = true;
    }

    public void start_quickScan() throws InterruptedException {
        //go to the query frame
        wd.switchTo().defaultContent();
        wd.switchTo().frame("leftFrame");
        wd.findElement(By.id("lnk489C877DAF13F79CE050A8C00D0E12D5")).click();//单元房
        //set up
        switchTab("单元房屋管理");
        backToMainFrame();
        if(DomExist(By.xpath("//span[@id='queryCondition_id_XZQH']//div[@class='selectize-input items not-full selectize-item-single-line']"))){
            //选择南海区
            wd.findElement(By.xpath("//span[@id='queryCondition_id_XZQH']//div[@class='selectize-input items not-full selectize-item-single-line']")).click();
            wd.findElement(By.xpath("//span[@id='queryCondition_id_XZQH']//input")).sendKeys(xingZhengqu);
            Thread.sleep(1500);
            wd.findElement(By.xpath("//div[@class='option active']")).click();

            wd.findElement(By.xpath("//span[@id='queryCondition_id_DZYSLXDM']//div[@class='selectize-input items not-full selectize-item-single-line']")).click();
            wd.findElement(By.xpath("//span[@id='queryCondition_id_DZYSLXDM']//input")).sendKeys("室");
            Thread.sleep(1500);
            wd.findElement(By.xpath("//div[@class='option active']")).click();
        }
        //选择左匹配
        wd.findElement(By.xpath("//table[@id='pageContent']//input[@value='left']")).click();

        //建筑物set up
        wd.switchTo().defaultContent();
        wd.switchTo().frame("leftFrame");
        wd.findElement(By.id("lnk489C877DAF12F79CE050A8C00D0E12D5")).click();//建筑物
        switchTab("建筑物管理");
        backToMainFrame();
        wd.findElement(By.xpath("//table[@id='pageContent']//input[@value='exact']")).click();//精确搜索

        for(;currentIndex<datas.size();currentIndex++){
            //全局current data
            data = datas.get(currentIndex);

            //是否存在正确的全称地址,不存在则去添加
            String fullname = govFullPrefix+data.sqmc;
            fullname+=data.jlx+data.mph;

            //若是小区则加上小区名字和楼栋
            if(data.isXQ()){
                fullname+=data.xqmc;
                fullname+=data.lph;
            }

            fullname+=data.jzwmc;
            wd.findElement(By.id("queryCondition_id_DZMC")).sendKeys(fullname);
            wd.findElement(By.id("_btnQueryID")).click();

            //没有则去添加
            if(!DomExist(By.linkText(fullname))){
                dataList.get(data.index).set(data.DONE_INDEX-1,"建筑物不存在！");//更新总数据表里的roomDone！
            }else{
                dataList.get(data.index).set(data.DONE_INDEX-1,"建筑物存在！");//更新总数据表里的roomDone！
            }
            wd.findElement(By.id("queryCondition_id_DZMC")).clear();

            //查询房间数量！
            if(data.hasRoom()){
                switchTab("单元房屋管理");
                backToMainFrame();
                wd.findElement(By.id("queryCondition_id_DZMC")).clear();
                wd.findElement(By.id("pageSize")).clear();
                //check
                //check data
                String[] fjhs = data.fjh.split("-");
                int len = fjhs[1].length();
                int floorStart = Integer.parseInt(fjhs[0].split("0")[0]);
                int floor = Integer.parseInt(data.lcs);
                int room = Integer.parseInt(fjhs[1].substring(len-2, len));
                int roomCount = (floor-floorStart+1)*room;

                wd.findElement(By.id("queryCondition_id_DZMC")).sendKeys(fullname);
                wd.findElement(By.id("pageSize")).sendKeys(String.valueOf(roomCount));
                wd.findElement(By.id("_btnQueryID")).click();


                if(!DomExist(By.partialLinkText(fullname))){
                    dataList.get(data.index).set(data.DONE_INDEX-2,"无房间！");//更新总数据表里的roomDone！
                }else{
                    var ems = wd.findElements(By.partialLinkText(fullname));
                    if(ems.size()==roomCount)
                        dataList.get(data.index).set(data.DONE_INDEX-2,"房间数正确："+String.valueOf(ems.size()));//更新总数据表里的roomDone！
                    else
                        dataList.get(data.index).set(data.DONE_INDEX-2,"欠房间数："+String.valueOf(roomCount-ems.size()));//更新总数据表里的roomDone！
                }
                switchTab("建筑物管理");
                backToMainFrame();
            }
        }
        isFinished = true;
    }

    public void startDeleteJZW() throws InterruptedException {
        //建筑物set up
        wd.switchTo().defaultContent();
        wd.switchTo().frame("leftFrame");
        wd.findElement(By.id("lnk489C877DAF12F79CE050A8C00D0E12D5")).click();//建筑物
        switchTab("建筑物管理");
        backToMainFrame();

        for(;currentIndex<datas.size();currentIndex++){
            //全局current data
            data = datas.get(currentIndex);

            //是否存在正确的全称地址,不存在则去添加
            String fullname = govFullPrefix+data.sqmc;
            fullname+=data.jlx+data.mph;

            //若是小区则加上小区名字和楼栋
            if(data.isXQ()){
                fullname+=data.xqmc;
                fullname+=data.lph;
            }

            fullname+=data.jzwmc;
            wd.findElement(By.xpath("//table[@id='pageContent']//input[@value='exact']")).click();//精确搜索
            wd.findElement(By.id("queryCondition_id_DZMC")).sendKeys(fullname);
            wd.findElement(By.id("_btnQueryID")).click();

            //没有
            if(!DomExist(By.linkText(fullname))){
                dataList.get(data.index).set(data.DONE_INDEX-1,"建筑物没找到！");//更新总数据表里的roomDone！
                reduntIndex.add(data.index);
            }else{
                wd.findElement(By.id("selectedRecord")).click();//选择记录
                wd.findElement(By.linkText("撤销")).click();
                //若有对话框提示房间号重复的话！
                if(wd.findElement(By.xpath("//div[@class='ui-dialog']//div[@i='content']")).getText().contains("确定是否撤销该记录") ){
                    wd.findElement(By.xpath("//button[@data-id='ok']")).click();
                    sucessIndexs.add(data.index);
                }
                dataList.get(data.index).set(data.DONE_INDEX-1,"建筑物删除！");//更新总数据表里的roomDone！
                backToMainFrame();
            }
            wd.findElement(By.id("queryCondition_id_DZMC")).clear();
        }
        isFinished = true;
    }

    public void processDWY() throws InterruptedException {
        if(data.hasRoom()){
            //第三类,是小区且有房子！
            if(queryJZW(data,false)){
                addRooms(data);
            }else{
                //建筑物不存在，搞搞他！
                if(queryXQ(data,false)){
                    addJZW(data);
                    currentIndex--;//回退重查！
                }else{
                    //小区变蓝色，则总点门牌号有冲突！
                    reduntIndex.add(data.index);
                }
            }
        }else{
            //独栋！
            if(queryJZW(data,true)) {
                //查到有就是重复咯
                reduntIndex.add(data.index);
            }else{
                //建筑物不存在，搞搞他！
                if(queryXQ(data,false)){
                    if(addJZW(data))
                        sucessIndexs.add(data.index);
                    else
                        reduntIndex.add(data.index);
                }else{
                    //小区变蓝色，则总点门牌号有冲突！
                    reduntIndex.add(data.index);
                }
            }
        }
    }

    public void processNormalPlate()throws InterruptedException{
        //先查有没有该街路向！
        if(queryJLX(data,false)){
            //有该街路向，是时候去加建筑物了
            if(addJZW(data)){
                //添加成功
                sucessIndexs.add(data.index);
                //当下条是一样街路向的话,超级循环添加！
                while(currentIndex<datas.size()-1 && !datas.get(currentIndex+1).hasRoom()){
                    if(datas.get(currentIndex+1).jlx.equals(data.jlx)){
                        data = datas.get(++currentIndex);//获取下条数据
                        //切换页面并重新点击
                        switchTab("查看街路巷(小区)");
                        wd.findElement(By.xpath("//form//input[@value='建筑物登记']")).click();
                        backToMainFrame();

                        if(addJZW(data)){
                            sucessIndexs.add(data.index);
                        }else{
                            reduntIndex.add(data.index);
                        }
                        output.setText("最后处理的一条："+data.index);
                    }else
                        break;
                }
            }else{
                reduntIndex.add(data.index);
                output.setText("最后处理的一条："+data.index);//当下条是一样街路向的话,超级循环添加！

                //当下条是一样街路向的话,超级循环添加！
                while(currentIndex<datas.size()-1 && !datas.get(currentIndex+1).hasRoom()){
                    if(datas.get(currentIndex+1).jlx.equals(data.jlx)){
                        data = datas.get(++currentIndex);//获取下条数据
                        //切换页面并重新点击
                        switchTab("查看街路巷(小区)");
                        wd.findElement(By.xpath("//form//input[@value='建筑物登记']")).click();
                        backToMainFrame();

                        if(addJZW(data)){
                            sucessIndexs.add(data.index);
                        }else{
                            reduntIndex.add(data.index);
                        }
                        output.setText("最后处理的一条："+data.index);
                    }else
                        break;
                }
            }
        }else{
            //假如没街路向，咱们先去添加嘛！
            addJLX(data);
            currentIndex--;//index回退，重新查一遍嘛！
        }
    }

    public void processChuzhuFang()throws InterruptedException{
        if(queryJZW(data,false)){
            addRooms(data);
        }else{
            //没有该建筑物呀,先查下街路向嘛
            if(queryJLX(data,false))
                addJZW(data);
            else {
                //若不存在，则添加后重新查！
                if (data.jlx.endsWith("里") || data.jlx.endsWith("坊") || data.jlx.endsWith("队") || data.jlx.endsWith("村集")) {
                    addQT(data, "生活区");
                } else if (data.jlx.endsWith("村")) {
                    addQT(data, "自然村");
                } else
                    addJLX(data);

                //新增后需要重新点击左边栏，重新加载出查询界面，
                wd.switchTo().defaultContent();
                wd.switchTo().frame("leftFrame");
                waitForLoad(By.id("itm489C877DAF10F79CE050A8C00D0E12D5"), 5);
                wd.findElement(By.id("lnk489C877DAF11F79CE050A8C00D0E12D5")).click();
                queryJLX(data, false);
                addJZW(data);

                //添加完后回退！
                currentIndex--;
            }
        }
    }

    public void processXQ() throws InterruptedException{
        if(data.hasRoom()){
            //第三类,是小区且有房子！
            if(queryJZW(data,false)){
                addRooms(data);
                //看下下条是不是同个栋的！
            }else{
                //建筑物不存在，搞搞他！
                if(queryXQ(data,false)){
                    addJZW(data);
                    currentIndex--;//回退重查！
                }else{
                    //小区变蓝色，则总点门牌号有冲突！
                    reduntIndex.add(data.index);
                }
            }
        }else{
            //独栋！
            if(queryJZW(data,true)){
                //查到有就是重复咯
                reduntIndex.add(data.index);
            }else{
                //建筑物不存在，搞搞他！
                if(queryXQ(data,false)){
                    if(addJZW(data))
                        sucessIndexs.add(data.index);
                    else
                        reduntIndex.add(data.index);
                }else{
                    //小区变蓝色，则总点门牌号有冲突！
                    reduntIndex.add(data.index);
                }
            }
        }
    }

    public Boolean queryJLX(DataBean data, Boolean notJump) throws InterruptedException {
        System.out.println("查询街路巷："+data.jlx);
        //打开查询分页！
        wd.switchTo().defaultContent();
        wd.switchTo().frame("leftFrame");
        waitForLoad(By.id("itm489C877DAF10F79CE050A8C00D0E12D5"),5);
        wd.findElement(By.id("lnk489C877DAF11F79CE050A8C00D0E12D5")).click();
        backToMainFrame();

        //添加筛选条件！
//        if(DomExist(By.xpath("//span[@id='queryCondition_id_SSSSXQ']//div[@class='selectize-input items not-full selectize-item-single-line']"))){
//            //选择南海区
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_SSSSXQ']//div[@class='selectize-input items not-full selectize-item-single-line']")).click();
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_SSSSXQ']//input")).sendKeys(xingZhengqu);
//            try {
//                Thread.sleep(1500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            wd.findElement(By.xpath("//div[@class='option active']")).click();
//
//            //选择西樵镇
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_SSXZJD']//div[@class='selectize-input items not-full selectize-item-single-line']")).click();
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_SSXZJD']//input")).sendKeys(zhen);
//            try {
//                Thread.sleep(1500);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            wd.findElement(By.xpath("//div[@class='option active']")).click();
//        }

        //是否存在正确的全称地址,不存在则去添加
        wd.findElement(By.xpath("//table[@id='pageContent']//input[@value='exact']")).click();//精确搜索
        String fullname = govFullPrefix+data.sqmc+data.jlx;

        //试下查全名，不然有些太短的查不出来，如‘北街’
        wd.findElement(By.id("queryCondition_id_DZMC")).sendKeys(fullname);
//        Thread.sleep(1000);
        wd.findElement(By.id("_btnQueryID")).click();

        if(!DomExist(By.linkText(fullname))){
            return false;
        }else{
            if(notJump)
                return true;
            //点击包含社区名的揭露像全称连接
            wd.findElement(By.linkText(fullname)).click();
            Thread.sleep(1000);
            switchTab("查看街路巷(小区)");
            wd.findElement(By.xpath("//form//input[@value='建筑物登记']")).click();
            backToMainFrame();
            return true;
        }
    }

    public Boolean queryXQ(DataBean data, boolean notAdd) throws InterruptedException{
        System.out.println("查询小区："+data.jlx+data.xqmc);
        //打开查询分页！
        wd.switchTo().defaultContent();
        wd.switchTo().frame("leftFrame");
//        waitForLoad(By.id("itm489C877DAF10F79CE050A8C00D0E12D5"),5);
        wd.findElement(By.id("lnk489C877DAF11F79CE050A8C00D0E12D5")).click();
        backToMainFrame();

//        //添加筛选条件！
//        if(DomExist(By.xpath("//span[@id='queryCondition_id_SSSSXQ']//div[@class='selectize-input items not-full selectize-item-single-line']"))){
//            //选择南海区
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_SSSSXQ']//div[@class='selectize-input items not-full selectize-item-single-line']")).click();
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_SSSSXQ']//input")).sendKeys(xingZhengqu);
//
//            Thread.sleep(1500);
//            wd.findElement(By.xpath("//div[@class='option active']")).click();
//
//            //选择西樵镇
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_SSXZJD']//div[@class='selectize-input items not-full selectize-item-single-line']")).click();
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_SSXZJD']//input")).sendKeys(zhen);
//
//            Thread.sleep(1500);
//            wd.findElement(By.xpath("//div[@class='option active']")).click();
//        }

        String xqFullName = govFullPrefix +data.sqmc+data.jlx+data.mph+data.xqmc;

        wd.findElement(By.xpath("//table[@id='pageContent']//input[@value='exact']")).click();//精确搜索
        wd.findElement(By.id("queryCondition_id_DZMC")).sendKeys(xqFullName);
        Thread.sleep(1500);
        wd.findElement(By.id("_btnQueryID")).click();

        if(notAdd){
            return DomExist(By.linkText(xqFullName));
        }

        if(!DomExist(By.linkText(xqFullName))){
            //若查询没有则去添加
            if(queryJLX(data,true)){
                if(addXQ(data))
                    queryXQ(data,false);
                else
                    return false;
            }else{
                //添加街路巷
                if(data.jlx.endsWith("里") || data.jlx.endsWith("坊") || data.jlx.endsWith("队")  || data.jlx.endsWith("村集")){
                    addQT(data,"生活区");
                }else if(data.jlx.endsWith("村")){
                    addQT(data,"自然村");
                }else
                    addJLX(data);

                //新增后需要重新点击左边栏，重新加载出查询界面，
                wd.switchTo().defaultContent();
                wd.switchTo().frame("leftFrame");
                waitForLoad(By.id("itm489C877DAF10F79CE050A8C00D0E12D5"),5);
                wd.findElement(By.id("lnk489C877DAF11F79CE050A8C00D0E12D5")).click();

                //添加完街路巷后，添加小区
                if(addXQ(data))
                    queryXQ(data,false);
                else
                    return false;
            }
        }else{
            wd.findElement(By.linkText(xqFullName)).click();
            switchTab("查看街路巷(小区)");
            wd.findElement(By.xpath("//form//input[@value='建筑物登记']")).click();
            backToMainFrame();
        }
        return true;
    }

    public Boolean queryJZW(DataBean data, Boolean notJump) throws InterruptedException {
        //go to the query frame
        wd.switchTo().defaultContent();
        wd.switchTo().frame("leftFrame");
        wd.findElement(By.id("lnk489C877DAF12F79CE050A8C00D0E12D5")).click();
        switchTab("建筑物管理");
        backToMainFrame();

//        //填筛选条件
//        if(DomExist(By.xpath("//span[@id='queryCondition_id_XZQH']//div[@class='selectize-input items not-full selectize-item-single-line']"))){
//            //选择南海区
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_XZQH']//div[@class='selectize-input items not-full selectize-item-single-line']")).click();
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_XZQH']//input")).sendKeys(xingZhengqu);
//
//            Thread.sleep(1500);
//            wd.findElement(By.xpath("//div[@class='option active']")).click();
//
//            //选择西樵镇
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_XZJD']//div[@class='selectize-input items not-full selectize-item-single-line']")).click();
//            wd.findElement(By.xpath("//span[@id='queryCondition_id_XZJD']//input")).sendKeys(zhen);
//
//            Thread.sleep(1500);
//            wd.findElement(By.xpath("//div[@class='option active']")).click();
//        }

        //是否存在正确的全称地址,不存在则去添加
        wd.findElement(By.xpath("//table[@id='pageContent']//input[@value='exact']")).click();//精确搜索
        String fullname = govFullPrefix+data.sqmc;
        fullname+=data.jlx+data.mph;

        //若是小区则加上小区名字和楼栋
        if(data.isXQ()){
            fullname+=data.xqmc;
            fullname+=data.lph;
        }

        fullname+=data.jzwmc;
        wd.findElement(By.id("queryCondition_id_DZMC")).sendKeys(fullname);
        wd.findElement(By.id("_btnQueryID")).click();

        //没有则去添加
        if(!DomExist(By.linkText(fullname))){
            return false;
        }else{
            if(notJump) return true;//不跳去单元房登记！
            var dzmcs = wd.findElement(By.linkText(fullname));
            dzmcs.click();
            backToMainFrame();
            waitForLoad(By.xpath("//form//input[@value='单元房屋登记']"),10);
            wd.findElement(By.xpath("//form//input[@value='单元房屋登记']")).click();
            switchTab("新增单元房屋");
            return true;
        }
    }

    public Boolean addRooms(DataBean data) throws InterruptedException {
//        Thread.sleep(500);
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.className("item"),10));
        //没房间号的是独栋！
        if(data.fjh.equals("")){
            //填楼层！这里可能有填不到的状况！
            wd.findElement(By.id("sslc")).click();
            wd.findElement(By.id("sslc")).sendKeys(data.lcs);

            //若单元号不为空，就填tm的单元号
            if(!data.dyh.equals("")){
                addRoom_fillDYH(data);
            }

            //尝试保存
            wd.findElement(By.xpath("//form//table//input[@value='保存']")).click();
            //若有对话框提示房间号重复的话！
            if(wd.findElement(By.xpath("//div[@class='ui-dialog']//div[@i='content']")).getText().equals("存在相同有效的地址名称,是否修改原单元房屋信息为现信息！"))
                wd.findElement(By.xpath("//button[@data-id='cancel']")).click();
            sucessIndexs.add(data.index);
            return true;
        }

        //check data
        String[] fjhs = data.fjh.split("-");
        int len = fjhs[1].length();
        int floorStart = Integer.parseInt(fjhs[0].split("0")[0]);

        //开始循环！
        int floor = Integer.parseInt(data.lcs);
        int room = Integer.parseInt(fjhs[1].substring(len-2, len));
        int roomCount = 0;

        for(int f = floorStart; f <= floor; f++){
            for(int r = 1; r <= room; r++){
                //若已做过则跳过
                System.out.println("已完成房间："+data.roomDone);
                if(roomCount < data.roomDone){
                    roomCount++;
                    System.out.println(f+"0"+r);
                    continue;
                }

                //填房间
                String sh;
                if(r<10)
                    sh=f+"0"+r+"房";
                else
                    sh=f+""+r+"房";

                wd.findElement(By.id("sh")).click();
                wd.findElement(By.id("sh")).sendKeys(sh);
                wd.findElement(By.id("bz")).sendKeys("房屋核实新增");


                //填楼层！这里可能有填不到的状况！
                wd.findElement(By.id("sslc")).click();
                wd.findElement(By.id("sslc")).sendKeys(String.valueOf(f));

                //若单元号不为空，就填tm的单元号
                if(!data.dyh.equals("")){
                    addRoom_fillDYH(data);
                }

                //尝试保存
                wd.findElement(By.xpath("//form//table//input[@value='保存']")).click();
                //若有对话框提示房间号重复的话！
                while(wd.findElement(By.xpath("//div[@class='ui-dialog']//div[@i='content']")).getText().equals("存在相同有效的地址名称,是否修改原单元房屋信息为现信息！")){
//                    Thread.sleep(500);
                    wd.findElement(By.xpath("//button[@data-id='cancel']")).click();
                    //重新填
                    String sh1;
                    if(++r<=room){
                        if(r<10)
                            sh1=f+"0"+r+"房";
                        else
                            sh1=f+""+r+"房";
                        wd.findElement(By.id("sh")).clear();
                        wd.findElement(By.id("sh")).sendKeys(sh1);
                        roomCount++;
                        data.roomDone = roomCount;
                        dataList.get(data.index).set(data.DONE_INDEX,data.roomDone);//更新总数据表里的roomDone！
                        output.setText("当前录到："+data.jlx+data.mph+data.xqmc+data.lph+" ---第"+data.roomDone+"个房间");
                        wd.findElement(By.xpath("//form//table//input[@value='保存']")).click();
                    }else if(++f<=floor){
                        //当层的录完了,换到下一层
                        wd.findElement(By.id("sslc")).click();
                        wd.findElement(By.id("sslc")).clear();
                        wd.findElement(By.id("sslc")).sendKeys(String.valueOf(f));

                        //重置房间号
                        r = 1;
                        sh1=f+"0"+r+"房";
                        wd.findElement(By.id("sh")).clear();
                        wd.findElement(By.id("sh")).sendKeys(sh1);
                        roomCount++;
                        data.roomDone = roomCount;
                        dataList.get(data.index).set(data.DONE_INDEX,data.roomDone);//更新总数据表里的roomDone！
                        output.setText("当前录到："+data.jlx+data.mph+data.xqmc+data.lph+" ---第"+data.roomDone+"个房间");
                        wd.findElement(By.xpath("//form//table//input[@value='保存']")).click();
                    }else
                        break;
                }

                roomCount++;
                data.roomDone = roomCount;
                dataList.get(data.index).set(data.DONE_INDEX,data.roomDone);//更新总数据表里的roomDone！

                //回去单元房登记分页
                switchTab("建筑物管理");
                wd.findElement(By.xpath("//form//input[@value='单元房屋登记']")).click();
                switchTab("新增单元房屋");
//                Thread.sleep(500);
                wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.className("item"),10));
            }
        }

        sucessIndexs.add(data.index);
        System.out.println(sucessIndexs);
        System.out.println("成功添加单元房！");
        return true;
    }

    public boolean addRoom_fillDYH(DataBean data) throws InterruptedException {
        //先尝试在下拉框里选号
        wd.findElement(By.xpath("//span[@id='dym_dzbm']//div[@class='selectize-input items not-full']")).click();
        Thread.sleep(1500);
        wd.findElement(By.xpath("//span[@id='dym_dzbm']//input")).sendKeys(data.dyh);

        String parentWindowId = wd.getWindowHandle();
        //若下拉框能正常显示。。。
        if(DomExist(By.xpath("//div[@class='option active']"))){
            //如果激活选项是对的直接选
            if(wd.findElement(By.xpath("//div[@class='option active']//div")).getText().equals(data.dyh)){
                wd.findElement(By.xpath("//div[@class='option active']")).click();
            }else if(DomExist(By.xpath("//div[@class='option']"))){
                boolean hasOption = false;
                for(WebElement em : wd.findElements(By.xpath("//div[@class='option']//div")))
                    if(em.getText().equals(data.dyh)){
                        hasOption = true;
                        break;
                    }

                if(hasOption){
                    //否则就换选项咯
                    while(!wd.findElement(By.xpath("//div[@class='option active']//div")).getText().equals(data.dyh))
                        wd.findElement(By.xpath("//span[@id='dym_dzbm']//input")).sendKeys(Keys.UP);
                    wd.findElement(By.xpath("//div[@class='option active']")).click();
                }else{
                    //下拉框没有,需要新增呢！
                    //添加单元号！
                    Thread.sleep(1000);
                    //用js点击按钮
                    ((JavascriptExecutor)wd).executeScript("setTimeout(function(){document.getElementById('addBtn').click()},100)");

                    //处理弹出页面
                    Set<String> allWindowsId = wd.getWindowHandles();
                    System.out.println("windos handle:");
                    boolean flag = true;
                    while(flag){
                        for(String windowId : allWindowsId){
                            System.out.println(windowId);
                            if(!windowId.equals(parentWindowId)){
                                wd.switchTo().window(windowId);
                                flag = false;
                                break;
                            }
                        }
                        allWindowsId = wd.getWindowHandles();
                    }

                    wd.findElement(By.id("dym")).sendKeys(data.dyh);
                    wd.findElement(By.xpath("//input[@value='确定']")).click();
                    wd.switchTo().window(parentWindowId);
                    backToMainFrame();

                    //这都还有对话框的话，有问题呀！
                    if(DomExist(By.xpath("//div[@class='ui-dialog']//div[@i='content']")))
                        return false;
                }
            }else{
                //mei options, xinzhen
                //下拉框没有,需要新增呢！
                //添加单元号！
                Thread.sleep(1000);
                //用js点击按钮
                ((JavascriptExecutor)wd).executeScript("setTimeout(function(){document.getElementById('addBtn').click()},100)");

                //处理弹出页面
                Set<String> allWindowsId = wd.getWindowHandles();
                System.out.println("windos handle:");
                boolean flag = true;
                while(flag){
                    for(String windowId : allWindowsId){
                        System.out.println(windowId);
//                        if(wd.switchTo().window(windowId).getTitle().equals("新增单元")){
                        if(!windowId.equals(parentWindowId)){
                            wd.switchTo().window(windowId);
                            flag = false;
                            break;
                        }
                    }
                    allWindowsId = wd.getWindowHandles();
                }

                wd.findElement(By.id("dym")).sendKeys(data.dyh);
                wd.findElement(By.xpath("//input[@value='确定']")).click();
                wd.switchTo().window(parentWindowId);
                backToMainFrame();

                //这都还有对话框的话，有问题呀！
                if(DomExist(By.xpath("//div[@class='ui-dialog']//div[@i='content']")))
                    return false;
            }
        }else{
            //下拉框没有,需要新增呢！
            //添加单元号！
            Thread.sleep(1000);
            //用js点击按钮
            ((JavascriptExecutor)wd).executeScript("setTimeout(function(){document.getElementById('addBtn').click()},100)");

            //处理弹出页面
            Set<String> allWindowsId = wd.getWindowHandles();
            System.out.println("windos handle:");
            boolean flag = true;
            while(flag){
                for(String windowId : allWindowsId){
                    System.out.println(windowId);
//                    if(wd.switchTo().window(windowId).getTitle().equals("新增单元")){
                    if(!windowId.equals(parentWindowId)){
                        wd.switchTo().window(windowId);
                        flag = false;
                        break;
                    }
                }
                allWindowsId = wd.getWindowHandles();
            }

            wd.findElement(By.id("dym")).sendKeys(data.dyh);
            wd.findElement(By.xpath("//input[@value='确定']")).click();
            wd.switchTo().window(parentWindowId);
            backToMainFrame();

            //这都还有对话框的话，有问题呀！
            if(DomExist(By.xpath("//div[@class='ui-dialog']//div[@i='content']")))
                return false;
        }
        return true;
    }

    public void addQT(DataBean data, String type) throws InterruptedException {
        //新增
        clickLink(By.linkText("新增"));
        //地址类型
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.className("item"),4));
        wd.findElement(By.xpath("//span[@id='dzyslxdm']//div[@class='selectize-input items not-full']")).click();
        wd.findElement(By.xpath("//span[@id='dzyslxdm']//input")).sendKeys(type);
        Thread.sleep(1500);
        wd.findElement(By.xpath("//div[@class='option active']")).click();

        //fill data
        wd.findElement(By.id("jlxxqmc")).sendKeys(data.jlx);
        //地址下拉框
        selectSQ();

        //警务室tree
        selectJWS();

        Thread.sleep(500);
        //多个社区的街路向
        clickSQ();

        //保存
        wd.findElement(By.id("saveButton")).click();
    }

    public void addJLX(DataBean data) throws InterruptedException {
        System.out.println("添加街路向");
        //新增街路向
        clickLink(By.linkText("新增街路巷"));

        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.className("item"),4));
        //fill data
        wd.findElement(By.id("jlxxqmc")).sendKeys(data.jlx);

        //社区下拉框
        selectSQ();

        Thread.sleep(500);
        //多个社区的街路向
        clickSQ();

        wd.findElement(By.id("saveButton")).click();
    }

    public Boolean addXQ(DataBean data) throws InterruptedException {
        //新增
        if(!data.dwm.equals("")){
            //新增
            clickLink(By.linkText("新增"));
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.className("item"),4));
            //地址类型
            wd.findElement(By.xpath("//span[@id='dzyslxdm']//div[@class='selectize-input items not-full']")).click();
            wd.findElement(By.xpath("//span[@id='dzyslxdm']//input")).sendKeys("单位（住宅）院");
            Thread.sleep(1000);
            wd.findElement(By.xpath("//div[@class='option active']")).click();
            wd.findElement(By.id("jlxxqmc")).click();
            wd.findElement(By.id("jlxxqmc")).sendKeys(data.dwm);
        }else{
            //新增
            clickLink(By.linkText("新增小区"));
            backToMainFrame();
            wd.findElement(By.id("jlxxqmc")).sendKeys(data.xqmc);
        }
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.className("item"),4));

        //社区下拉框
        selectSQ();

        //所属街路向
        Thread.sleep(1000);
        wd.findElement(By.xpath("//span[@id='ssjlxxq_dzbm']//div[@class='selectize-input items required not-full']")).click();
        wd.findElement(By.xpath("//span[@id='ssjlxxq_dzbm']//input")).sendKeys(data.sqmc+data.jlx);
        if(DomExist(By.xpath("//div[@class='option active']"))){
            if(wd.findElement(By.xpath("//div[@class='option active']//div")).getText().equals(data.sqmc+data.jlx))
                wd.findElement(By.xpath("//div[@class='option active']")).click();
            else{
                while(!wd.findElement(By.xpath("//div[@class='option active']//div")).getText().equals(data.sqmc+data.jlx))
                    wd.findElement(By.xpath("//span[@id='ssjlxxq_dzbm']//input")).sendKeys(Keys.UP);
                wd.findElement(By.xpath("//div[@class='option active']")).click();
            }
        }else
            return false;

        //警务室tree
        selectJWS();

        //先尝试在下拉框里选号
        addJZW_fillMPH(data.mph);

        wd.findElement(By.id("mlpzzb")).sendKeys(data.lon);
        wd.findElement(By.id("mlphzb")).sendKeys(data.lat);

        //多个社区的街路向
        clickSQ();

        //保存
        wd.findElement(By.id("saveButton")).click();
        if(DomExist(By.xpath("//div[@class='ui-dialog']//div[@i='content']"))){
            dataList.get(data.index).set(data.DONE_INDEX-1,wd.findElement(By.xpath("//div[@class='ui-dialog']//div[@i='content']")).getText());//更新总数据表里的roomDone！
        }

        return true;
    }

    public boolean addJZW(DataBean data) throws InterruptedException {
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.className("item"),6));
        //若不是揭露像，则不用填警务室社区 zaglssjwzrqdm_label
        String j = wd.findElement(By.xpath("//span[@id='zaglssjwzrqdm_span']//textarea")).getAttribute("title");
        if(!j.equals(data.getJWS())){
            selectJWS();
        }

        if(inputJZWData(data)){
            wd.findElement(By.xpath("//form//table//input[@value='保存']")).click();
            dataList.get(data.index).set(data.DONE_INDEX-1,wd.findElement(By.xpath("//div[@class='ui-dialog']//div[@i='content']")).getText());//更新总数据表里的roomDone！
            return true;
        }else{
            //门牌号有冲突呀！
            return false;
        }
    }

    public void selectJWS(){
        WebElement jws = wd.findElement(By.id("zaglssjwzrqdm_span"));
        jws.click();
        wait.until(ExpectedConditions.visibilityOf(wd.findElement(By.id("zaglssjwzrqdm_selectable_1_switch"))));
        WebElement jwcitem = wd.findElement(By.id("zaglssjwzrqdm_selectable_1_switch"));
        jwcitem.click();
        wait.until(ExpectedConditions.visibilityOf(wd.findElement(By.xpath("//div[@id='zaglssjwzrqdm_span_tree_container']//a[@title='"+data.getJWS()+"']"))));
        WebElement select = wd.findElement(By.xpath("//div[@id='zaglssjwzrqdm_span_tree_container']//a[@title='"+data.getJWS()+"']"));
        select.click();
        WebElement yes = wd.findElement(By.xpath("//div[@id='zaglssjwzrqdm_span_tree_container']//input[@value='确定']"));
        yes.click();
    }

    public void selectSQ(){
        //行政下拉框
        wd.findElement(By.id("sszdyjxzqy_dzbm_label")).click();
        new WebDriverWait(wd,10).until(ExpectedConditions.visibilityOf(wd.findElement(By.id("sszdyjxzqy_dzbm_selectable_1_switch"))));
        wd.findElement(By.id("sszdyjxzqy_dzbm_selectable_1_switch")).click();
        //西樵为3，丹灶为2
        new WebDriverWait(wd,10).until(ExpectedConditions.visibilityOf(wd.findElement(By.id("sszdyjxzqy_dzbm_selectable_2_switch"))));
        wd.findElement(By.id("sszdyjxzqy_dzbm_selectable_2_switch")).click();
        new WebDriverWait(wd,10).until(ExpectedConditions.visibilityOf(wd.findElement(By.xpath("//div[@id='sszdyjxzqy_span_tree_container']//a[@title='"+data.sqmc+"']"))));
        wd.findElement(By.xpath("//div[@id='sszdyjxzqy_span_tree_container']//a[@title='"+data.sqmc+"']")).click();
        wd.findElement(By.xpath("//div[@id='sszdyjxzqy_span_tree_container']//input[@value='确定']")).click();
    }

    public void clickSQ(){
        wd.findElement(By.id("sfbhdgjcw_0")).click();
        wait.until(ExpectedConditions.elementToBeClickable(wd.findElement(By.id("bhsq_span"))));
        wd.findElement(By.id("bhsq_span")).click();
        wait.until(ExpectedConditions.visibilityOf(wd.findElement(By.xpath("//div[@id='bhsq_span_tree_container']//span[@id='bhsq_selectable_1_span']"))));
        wd.findElement(By.xpath("//div[@id='bhsq_span_tree_container']//span[@id='bhsq_selectable_1_span']/..")).click();
        wd.findElement(By.xpath("//div[@id='bhsq_span_tree_container']//input[@value='确定']")).click();
    }

    public boolean addJZW_fillMPH(String mlph) throws InterruptedException {
        //填写编制门牌号页面
        //先尝试在下拉框里选号
        wd.findElement(By.xpath("//span[@id='mlp']//div[@class='selectize-input items not-full']")).click();
        Thread.sleep(1000);
        wd.findElement(By.xpath("//span[@id='mlp']//input")).sendKeys(mlph);
        Thread.sleep(1000);


        String parentWindowId = wd.getWindowHandle();
        //若下拉框能正常显示
        if(DomExist(By.xpath("//div[@class='option active']"))){
            //如果激活选项是对的直接选
            if(wd.findElement(By.xpath("//div[@class='option active']//div")).getText().equals(mlph)){
                wd.findElement(By.xpath("//div[@class='option active']")).click();
            }else{
                boolean hasoption = false;
                for(var em : wd.findElements(By.xpath("//div[@class='option']//div"))){
                    if(em.getText().equals(mlph)){
                        hasoption = true;
                        break;
                    }
                }

                if(hasoption){
                    //否则就换选项咯
                    while(!wd.findElement(By.xpath("//div[@class='option active']//div")).getText().equals(mlph))
                        wd.findElement(By.xpath("//span[@id='mlp']//input")).sendKeys(Keys.UP);
                    System.out.println("click:"+wd.findElement(By.xpath("//div[@class='option active']")).getText());
                    wd.findElement(By.xpath("//div[@class='option active']")).click();

                    if(DomExist(By.xpath("//div[@class='ui-dialog']//div[@i='content']"))){
                        dataList.get(data.index).set(data.DONE_INDEX-1,wd.findElement(By.xpath("//div[@class='ui-dialog']//div[@i='content']")).getText());//更新总数据表里的roomDone！
                        return false;
                    }
                }else{
                    //门牌号
                    //用js点击按钮
                    ((JavascriptExecutor)wd).executeScript("setTimeout(function(){document.getElementById('bz_btn').click()},100)");

                    //处理弹出页面
                    Set<String> allWindowsId = wd.getWindowHandles();
                    System.out.println("windos handle:");
                    boolean flag = true;
                    while(flag){
                        for(String windowId : allWindowsId){
                            System.out.println(windowId);
                            if(!windowId.equals(parentWindowId)){
                                wd.switchTo().window(windowId);
                                flag = false;
                                break;
                            }
                        }
                        allWindowsId = wd.getWindowHandles();
                    }

                    wd.findElement(By.id("mlph")).sendKeys(mlph);
                    wd.findElement(By.xpath("//input[@name='button']")).click();

                    //成功填完门牌号，回到主页面(理论上先查完建筑物确实没有的话，不考虑门牌重复的情况)
                    if(DomExist(By.xpath("//div[@class='ui-dialog']//div[@i='content']"))){
                        dataList.get(data.index).set(data.DONE_INDEX-1,wd.findElement(By.xpath("//div[@class='ui-dialog']//div[@i='content']")).getText()+"ERROR!");//更新总数据表里的roomDone！
//                        ((JavascriptExecutor)wd).executeScript("window.close()");
                        wd.switchTo().window(parentWindowId);
                        backToMainFrame();
                        return false;
                    }

                    wd.switchTo().window(parentWindowId);
                    backToMainFrame();
                }
            }
        }else{
            //门牌号
            //用js点击按钮
            ((JavascriptExecutor)wd).executeScript("setTimeout(function(){document.getElementById('bz_btn').click()},100)");

            //处理弹出页面
            Set<String> allWindowsId = wd.getWindowHandles();
            System.out.println("windos handle:");
            boolean flag = true;
            while(flag){
                for(String windowId : allWindowsId){
                    System.out.println(windowId);
                    if(!windowId.equals(parentWindowId)){
                        wd.switchTo().window(windowId);
                        flag = false;
                        break;
                    }
                }
                allWindowsId = wd.getWindowHandles();
            }

            wd.findElement(By.id("mlph")).sendKeys(mlph);
            wd.findElement(By.xpath("//input[@name='button']")).click();

            //成功填完门牌号，回到主页面(理论上先查完建筑物确实没有的话，不考虑门牌重复的情况)
            if(DomExist(By.xpath("//div[@class='ui-dialog']//div[@i='content']"))){
//                ((JavascriptExecutor)wd).executeScript("window.close()");
                dataList.get(data.index).set(data.DONE_INDEX-1,wd.findElement(By.xpath("//div[@class='ui-dialog']//div[@i='content']")).getText());//更新总数据表里的roomDone！
                wd.switchTo().window(parentWindowId);
                backToMainFrame();

                return false;
            }
            wd.switchTo().window(parentWindowId);
            backToMainFrame();
        }

        if(DomExist(By.xpath("//div[@class='ui-dialog']//div[@i='content']"))){
            dataList.get(data.index).set(data.DONE_INDEX-1,wd.findElement(By.xpath("//div[@class='ui-dialog']//div[@i='content']")).getText());//更新总数据表里的roomDone！
            return false;
        }
        return true;
    }

    public boolean inputJZWData(DataBean data) throws InterruptedException {
        String mlph;
        if(!data.isXQ())
            mlph = data.mph;
        else
            mlph = data.lph;
        if(addJZW_fillMPH(mlph)){
            //填写其他参数
            //坐标
            Thread.sleep(1000);
            wd.findElement(By.id("mlpzzb")).clear();
            wd.findElement(By.id("mlpzzb")).sendKeys(data.lon);
            wd.findElement(By.id("mlphzb")).clear();
            wd.findElement(By.id("mlphzb")).sendKeys(data.lat);

            //显示建筑物名称checkbox
            if(data.jzwmc.equals("")){
                wd.findElement(By.id("jzwdzmc_xsjzw_0")).click();
            }
            else
                wd.findElement(By.id("jzwmc")).sendKeys(data.jzwmc);

            //有房子，不是独栋咯！
            if(!data.hasRoom())
                wd.findElement(By.id("sfdtfwjzw_0")).click();
            if(!data.mplx.equals("蓝牌"))
                wd.findElement(By.id("bz")).sendKeys("房屋核实新增");
        }else{
            return false;
        }
        return true;
    }

    public void setUpDatas(){
        for(int i = 1; i < dataList.size();i++){
            //若是表格的无效数据则跳过
            if(isNotVaild(i)){
                continue;
            }

            //建立
            var row = dataList.get(i);
            DataBean data = new DataBean(isBlue,row,i);
            data.setIndex();
            if(data.setData())
                datas.add(data);
        }
    }

    public boolean isNotVaild(int i){
        return sucessIndexs.contains(i) || reduntIndex.contains(i);
    }
//
//    public void setRELOAD_MAX(int t){
//        RELOAD_MAX = t;
//    }

    public void clickLink(By locator){
        var link = wd.findElement(locator);
        while(DomExist(locator)){
            link.click();
            backToMainFrame();
        }
    }

    public void stop(){
        isFinished=true;
        currentIndex = datas.size();
    }

    public void reload(){
        System.out.println("reload");
        wd.navigate().refresh();
        new WebDriverWait(wd, 10).until(ExpectedConditions.alertIsPresent());
        wd.switchTo().alert().accept();
//        login();

        //到达应用界面了，展开左侧菜单先
        //set up
        wd.switchTo().frame("leftFrame");
        System.out.println(wd.getTitle());
        waitForLoad(By.id("itm489C877DAF10F79CE050A8C00D0E12D5"),5);
        WebElement menu = wd.findElement(By.id("itm489C877DAF10F79CE050A8C00D0E12D5"));
        menu.click();
    }

    public void setUpDriver(){
        //        System.setProperty("webdriver.ie.driver","D:\\data\\IEDriverServer32.exe");
//        DesiredCapabilities ieCapabilities = DesiredCapabilities.internetExplorer();
//        ieCapabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
//        wd = new InternetExplorerDriver(ieCapabilities);
        System.setProperty("webdriver.chrome.driver","C:\\Program Files (x86)\\Google Chrome\\chromedriver.exe");
        wd = new ChromeDriver();
        wd.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        wait = new WebDriverWait(wd,10);
    }

    public void waitForLoad(final By locator, int timeOut){
        WebDriverWait wait = new WebDriverWait(wd, timeOut);
        wait.until((ExpectedCondition<Object>) webDriver -> webDriver.findElement(locator));
    }

    public void login(){
        waitForLoad(By.id("username"),5);

        WebElement username = wd.findElement(By.name("username"));
        WebElement password = wd.findElement(By.name("password"));
        WebElement submit = wd.findElement(By.name("submit3"));

        username.sendKeys(USR);
        password.sendKeys(PSW);
        submit.click();
    }

    public void switchTab(String tabName){
        //test switch to formal tab
        System.out.println("切换到"+tabName);
        wd.switchTo().defaultContent();
        wd.switchTo().frame("mainFrame");
        wd.findElement(By.xpath("//span[contains(text(),'"+tabName+"')]/..")).click();
        backToMainFrame();
    }

    public boolean DomExist(By selector){
        System.out.println("Domexist:   "+selector.toString());
        wd.manage().timeouts().implicitlyWait(1,TimeUnit.SECONDS);
        try{
            wd.findElement(selector);
            System.out.println("find dom!");
            wd.manage().timeouts().implicitlyWait(3,TimeUnit.SECONDS);
            return true;
        }catch (Exception e){
            System.out.println("cant find dom!");
            wd.manage().timeouts().implicitlyWait(3,TimeUnit.SECONDS);
            return false;
        }

        //新版本试下
//        var ems = wd.findElements(selector);
//        return ems.size()>0;
    }

    public void backToMainFrame(){
        wd.switchTo().defaultContent();
        wd.switchTo().frame("mainFrame");
        wd.switchTo().frame(wd.findElement(By.xpath("//div[@id='mainDiv']//div[contains(@style,'block')]//iframe")));
    }
}
