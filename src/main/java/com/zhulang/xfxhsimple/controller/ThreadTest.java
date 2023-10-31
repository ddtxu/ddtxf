package com.zhulang.xfxhsimple.controller;

import cn.hutool.core.util.StrUtil;
import com.zhulang.xfxhsimple.component.XfXhStreamClient;
import com.zhulang.xfxhsimple.config.XfXhConfig;
import com.zhulang.xfxhsimple.dto.MsgDTO;
import com.zhulang.xfxhsimple.listener.XfXhWebSocketListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
@Slf4j
@Data
public class ThreadTest implements Callable<String> {

    @Resource
    private XfXhStreamClient xfXhStreamClient;

    @Resource
    private XfXhConfig xfXhConfig;

    private String message;//可以是字符串

    private String asrAddr;

    private String appid;

    public ThreadTest(){
    }
    public ThreadTest(String message, XfXhStreamClient xfXhStreamClient, XfXhConfig xfXhConfig, String asrAddr, String appid){
        this.message=message;
        this.xfXhStreamClient=xfXhStreamClient;
        this.xfXhConfig=xfXhConfig;
        this.asrAddr=asrAddr;
        this.appid=appid;
    }

    public ThreadTest(String message, XfXhStreamClient xfXhStreamClient, XfXhConfig xfXhConfig){
        this.message=message;
        this.xfXhStreamClient=xfXhStreamClient;
        this.xfXhConfig=xfXhConfig;

    }
    @Override
    public String call() throws Exception {
        return sendQuestion(message);
    }
    private String sendQuestion( String question) {
        String answer = "";
        Map<String, Object> resultMap = new HashMap<>();
        // 如果是无效字符串，则不对大模型进行请求
        if (StrUtil.isBlank(question)) {
            log.info("提问讯飞大模型 无效问题，请重新输入  ===>{}", question);
            return  "无效问题，请重新输入";
        }
        // 获取连接令牌
        if (!xfXhStreamClient.operateToken(XfXhStreamClient.GET_TOKEN_STATUS)) {
            log.info("提问讯飞大模型 当前大模型连接数过多，请稍后再试  ===>{}", question);
            return  "当前大模型连接数过多，请稍后再试";
        }

        // 创建消息对象
        MsgDTO msgDTO = MsgDTO.createUserMsg(question);
        // 创建监听器
        XfXhWebSocketListener listener = new XfXhWebSocketListener();
        // 发送问题给大模型，生成 websocket 连接
        WebSocket webSocket = xfXhStreamClient.sendMsg(UUID.randomUUID().toString().substring(0, 10), Collections.singletonList(msgDTO), listener);
        if (webSocket == null) {
            // 归还令牌
            xfXhStreamClient.operateToken(XfXhStreamClient.BACK_TOKEN_STATUS);
            log.info("提问讯飞大模型 系统内部错误，请联系管理员  ===>{}", question);
            answer = "系统内部错误，请联系管理员";
        }
        try {
            int count = 0;
            // 为了避免死循环，设置循环次数来定义超时时长
            int maxCount = xfXhConfig.getMaxResponseTime() * 5;
            while (count <= maxCount) {
                Thread.sleep(200);
                if (listener.isWsCloseFlag()) {
                    break;
                }
                count++;
            }
            if (count > maxCount) {
                log.info("大模型响应超时，请联系管理员  ===>{}", question);
                return  "大模型响应超时，请联系管理员";
            }
            // 响应大模型的答案
           return answer = listener.getAnswer().toString();
        } catch (InterruptedException e) {
            log.error("错误：" + e.getMessage());
            log.info("提问讯飞大模型 系统内部错误，请联系管理员 ===>{}", e.getMessage());
            return answer = "系统内部错误，请联系管理员";
        } finally {
            // 关闭 websocket 连接
            webSocket.close(1000, "");
            // 归还令牌
            xfXhStreamClient.operateToken(XfXhStreamClient.BACK_TOKEN_STATUS);
        }
    }


}
