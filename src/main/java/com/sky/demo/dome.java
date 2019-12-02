package com.sky.demo;
import com.sky.demo.tool.AliyunIoTSignUtil;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.util.HashMap;
import java.util.Map;

public class dome {

    public static String productKey = "********";
    public static String deviceName = "OpenMQTTDevice";
    public static String deviceSecret = "********";
    public static String regionId = "cn-shanghai";

    // 物模型-属性上报topic
    private static String pubTopic = "/sys/" + productKey + "/" + deviceName + "/thing/event/property/post";
    // 自定义topic，在产品Topic列表位置定义
    private static String subTopic = "/"+productKey + "/" + deviceName+"/user/newdatademo";

    private static MqttClient mqttClient;

    public static void main(String [] args){

        initAliyunIoTClient();
//        ScheduledExecutorService scheduledThreadPool = new ScheduledThreadPoolExecutor(1,
//                new ThreadFactoryBuilder().setNameFormat("thread-runner-%d").build());
//
//        scheduledThreadPool.scheduleAtFixedRate(()->postDeviceProperties(), 10,10, TimeUnit.SECONDS);
        // 汇报属性
        postDeviceProperties();
        try {
            mqttClient.subscribe(subTopic); // 订阅Topic
        } catch (MqttException e) {
            System.out.println("error:" + e.getMessage());
            e.printStackTrace();
        }

        // 设置订阅监听
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                System.out.println("connection Lost");

            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                System.out.println("Sub message");
                System.out.println("Topic : " + s);
                System.out.println(new String(mqttMessage.getPayload())); //打印输出消息payLoad
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });

    }

    /**
     * 初始化 Client 对象
     */
    private static void initAliyunIoTClient() {

        try {
            // 构造连接需要的参数
            String clientId = "java" + System.currentTimeMillis();
            Map<String, String> params = new HashMap<>(16);
            params.put("productKey", productKey);
            params.put("deviceName", deviceName);
            params.put("clientId", clientId);
            String timestamp = String.valueOf(System.currentTimeMillis());
            params.put("timestamp", timestamp);
            // cn-shanghai
            String targetServer = "tcp://" + productKey + ".iot-as-mqtt."+regionId+".aliyuncs.com:1883";

            String mqttclientId = clientId + "|securemode=3,signmethod=hmacsha1,timestamp=" + timestamp + "|";
            String mqttUsername = deviceName + "&" + productKey;
            String mqttPassword = AliyunIoTSignUtil.sign(params, deviceSecret, "hmacsha1");

            connectMqtt(targetServer, mqttclientId, mqttUsername, mqttPassword);

        } catch (Exception e) {
            System.out.println("initAliyunIoTClient error " + e.getMessage());
        }
    }

    public static void connectMqtt(String url, String clientId, String mqttUsername, String mqttPassword) throws Exception {

        MemoryPersistence persistence = new MemoryPersistence();
        mqttClient = new MqttClient(url, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        // MQTT 3.1.1
        connOpts.setMqttVersion(4);
        connOpts.setAutomaticReconnect(false);
        connOpts.setCleanSession(true);

        connOpts.setUserName(mqttUsername);
        connOpts.setPassword(mqttPassword.toCharArray());
        connOpts.setKeepAliveInterval(60);

        mqttClient.connect(connOpts);
    }

    /**
     * 汇报属性
     */
    private static void postDeviceProperties() {

        try {
            //上报数据
            //高级版 物模型-属性上报payload
            System.out.println("上报属性值");
            String payloadJson = "{\"params\":{\"Status\":0,\"Data\":\"15\"}}";
            MqttMessage message = new MqttMessage(payloadJson.getBytes("utf-8"));
            message.setQos(1);
            mqttClient.publish(pubTopic, message);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}