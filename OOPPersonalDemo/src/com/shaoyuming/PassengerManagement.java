package com.shaoyuming;

import java.io.*;
import java.sql.*;
import java.util.*;
//作者：计算机互认231-08-邵昱铭
//小组大主题：机场管理系统  子主题：乘客管理

//使用通用接口 航班服务保证所有乘客（无论是普通乘客还是VIP乘客）都能执行相应的服务操作
interface FlightService {
    void checkIn(); //值机
    void boardFlight(); //登机
    void inFlightService(); //机上服务
    void postFlightFeedback(); //乘机后评价
}

//普通乘客类
class Passenger implements FlightService, Serializable {
    protected String name; //姓名
    protected String gender; //性别
    protected String id; //身份证
    protected String nationality; //国籍
    protected List<String> boardingHistory; //登机历史（List）

    public Passenger(String name, String gender, String id, String nationality) {
        this.name = name;
        this.gender = gender;
        this.id = id;
        this.nationality = nationality;
        this.boardingHistory = new ArrayList<>();
    }

    //输出部分的方法重写
    @Override
    public void checkIn() {
        System.out.println(name + " 已完成值机。");
    }

    @Override
    public void boardFlight() {
        System.out.println(name + " 已登机。");
    }

    @Override
    public void inFlightService() {
        System.out.println("普通乘客 " + name + " 正在享受机上服务。");
    }

    @Override
    public void postFlightFeedback() {
        System.out.println(name + " 已完成乘后评价。");
    }

    public String toString() {
        return "乘客信息：{姓名：" + name + ", 性别：" + gender + ", 身份证号：" + id +
                ", 国籍：" + nationality + "}";
    }
}

//VIP乘客类 继承乘客类继承了普通乘客的基本功能
class VIPPassenger extends Passenger {
    int vipPoints;
    int starLevel;

    public VIPPassenger(String name, String gender, String id, String nationality, int vipPoints, int starLevel) {
        super(name, gender, id, nationality);
        this.vipPoints = vipPoints;
        this.starLevel = starLevel;
    }
    //重写了 checkIn()、boardFlight()和inFlightService()方法，提供VIP乘客专有的优先服务。
    @Override
    public void checkIn() {
        System.out.println("VIP乘客 " + name + " VIP乘客已完成优先值机。");
    }

    @Override
    public void boardFlight() {
        System.out.println("VIP乘客 " + name + " VIP乘客已完成优先登机。");
    }

    @Override
    public void inFlightService() {
        System.out.println("VIP乘客 " + name + " VIP乘客正在享受专属机上服务。");
    }

    public String toString() {
        return "***VIP乘客信息***：{" +
                "姓名：" + name + ", 性别：" + gender + ", 身份证号：" + id +
                ", 国籍：" + nationality + ", VIP积分：" + vipPoints +
                ", VIP星级：" + starLevel + "}";
    }
}

//乘客管理类 负责管理乘客的信息，提供了增、删、改、查等操作
class PassengerManager {
    private List<Passenger> passengers = new ArrayList<>();

    //添加乘客
    public void addPassenger(Passenger passenger) {
        passengers.add(passenger);
    }

    //遍历所有乘客并打印其信息
    public void displayPassengers() {
        for (Passenger p : passengers) {
            System.out.println(p);
        }
    }

    //IO流保存乘客信息到文件
    public void saveToFile(String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(passengers);
        }
    }

    //IO从文件加载乘客信息
    @SuppressWarnings("unchecked")
    public void loadFromFile(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            passengers = (List<Passenger>) ois.readObject();
        }
    }

    //保存到数据库（在此程序中我使用MySQL数据库）
    public void saveToDatabase(Connection conn) throws SQLException {
        String createTable = "CREATE TABLE IF NOT EXISTS passengers (" +
                "name VARCHAR(100) NOT NULL, " +
                "gender VARCHAR(10) NOT NULL, " +
                "id VARCHAR(20) PRIMARY KEY, " +
                "nationality VARCHAR(50) NOT NULL, " +
                "type VARCHAR(10) NOT NULL, " +
                "vipPoints INT DEFAULT 0, " +
                "starLevel INT DEFAULT 0)";
        conn.createStatement().execute(createTable);

        String sql = "INSERT INTO passengers (name, gender, id, nationality, type, vipPoints, starLevel) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name=VALUES(name), gender=VALUES(gender), nationality=VALUES(nationality), type=VALUES(type), vipPoints=VALUES(vipPoints), starLevel=VALUES(starLevel)";

        //异常处理
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Passenger p : passengers) {
                ps.setString(1, p.name);
                ps.setString(2, p.gender);
                ps.setString(3, p.id);
                ps.setString(4, p.nationality);
                ps.setString(5, p instanceof VIPPassenger ? "VIP" : "Regular");

                if (p instanceof VIPPassenger) {
                    VIPPassenger vip = (VIPPassenger) p;
                    ps.setInt(6, vip.vipPoints);
                    ps.setInt(7, vip.starLevel);
                } else {
                    ps.setInt(6, 0);
                    ps.setInt(7, 0);
                }
                ps.executeUpdate();
            }
        }
    }

    //从MySQL数据库加载数据
    public void loadFromDatabase(Connection conn) throws SQLException {
        passengers.clear();
        String sql = "SELECT * FROM passengers";
        //异常处理
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("name");
                String gender = rs.getString("gender");
                String id = rs.getString("id");
                String nationality = rs.getString("nationality");
                String type = rs.getString("type");

                if ("VIP".equals(type)) {//只有VIP才读
                    int vipPoints = rs.getInt("vipPoints");  // 读取VIP积分
                    int starLevel = rs.getInt("starLevel");  // 读取VIP星级
                    passengers.add(new VIPPassenger(name, gender, id, nationality, vipPoints, starLevel));
                } else {
                    passengers.add(new Passenger(name, gender, id, nationality));
                }
            }
        }
    }
}

//主程序：乘客管理
public class PassengerManagement {
    public static void main(String[] args) {
        PassengerManager manager = new PassengerManager();

        //创建普通乘客和VIP乘客两个对象
        Passenger p1 = new Passenger("张三", "女", "1090105165494512", "美国");
        VIPPassenger VIP_p1 = new VIPPassenger("邵昱铭", "男", "210548411656516", "中国", 1700, 5);

        //添加乘客到管理系统 测试数据库存入功能
        manager.addPassenger(p1);
        manager.addPassenger(VIP_p1);

        //调用乘客服务的四个方法
        System.out.println("开始处理普通乘客：");
        p1.checkIn(); //直接调用接口中的方法
        p1.boardFlight();
        p1.inFlightService();
        p1.postFlightFeedback();
        System.out.println("--------------------");

        System.out.println("\n开始处理VIP乘客：");
        VIP_p1.checkIn();
        VIP_p1.boardFlight();
        VIP_p1.inFlightService();
        VIP_p1.postFlightFeedback();
        System.out.println("--------------------");

        //数据库配置
        String dbUrl = "jdbc:mysql://localhost:3306/passengers_management";
        String dbUser = "root";
        String dbPassword = "123456";

        //异常处理嵌套数据库读写
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            manager.saveToDatabase(conn);
            System.out.println("数据已保存到数据库。");
            System.out.println("--------------------");
            manager.loadFromDatabase(conn);
            System.out.println("从数据库加载后:");
            manager.displayPassengers();
            System.out.println("--------------------");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //异常处理嵌套IO文件读写
        try {
            manager.saveToFile("passengers.dmg");
            manager.loadFromFile("passengers.dmg");
            System.out.println("从文件加载后:");
            manager.displayPassengers();
            System.out.println("--------------------");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

//by 08 邵昱铭