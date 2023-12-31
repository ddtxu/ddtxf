package com.zhulang.xfxhsimple.controller;

import cn.hutool.core.util.StrUtil;
import com.zhulang.xfxhsimple.component.XfXhStreamClient;
import com.zhulang.xfxhsimple.config.XfXhConfig;
import com.zhulang.xfxhsimple.dto.MsgDTO;
import com.zhulang.xfxhsimple.listener.XfXhWebSocketListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/testone")
@Slf4j
public class DDTestController {

    @Resource
    private XfXhStreamClient xfXhStreamClient;

    @Resource
    private XfXhConfig xfXhConfig;

    @PostMapping("/flowtest")
    public Map<String, Object> flow(@RequestBody Map<String, Object> tokenMap) throws Exception {
        String asrAddr = "127.0.0.1:9988";
        String ttsAddr = "ws://127.0.0.1:9989/tts";
        Long timestamp = (Long) tokenMap.get("timestamp");
        String method = (String) tokenMap.get("method");
        String callid = (String) tokenMap.get("callid");
        String appid = (String) tokenMap.get("appid");
        Map<String, Object> resultMap = new HashMap<>();
        if ("create".equals(method)) {
            String callSource = (String) tokenMap.get("call_source");
            log.info("callSource ===>{}", callSource);
            String sourceName = (String) tokenMap.get("source_name");
            log.info("sourceName ===>{}", sourceName);
            Map<String, Object> ttsMap = new HashMap<>();
            resultMap.put("action", "cti_play_and_detect_speech");
            String date = LocalDateTime.now().toString();
            resultMap.put("argument", "'1' '64' '0' '0.8' '" + asrAddr + "' '120' '800' '5000' '20000' '' '' '" + appid + "' '1' '" + date + "' 'wav'");
            ttsMap.put("ttsurl", ttsAddr);
            ttsMap.put("ttsvoicename", "x4_lingxiaoxuan_en");
            ttsMap.put("ttsconfig", "");
            ttsMap.put("ttsengine", "");
            ttsMap.put("ttsvolume", 0);
            ttsMap.put("ttsspeechrate", 0);
            ttsMap.put("ttspitchrate", 0);
            resultMap.put("tts", ttsMap);
            resultMap.put("privatedata", "test");
            List<String> list = Arrays.asList("欢迎进入顶顶通对接讯飞大模型的程序", "请问您有什么问题想向我提问的呢？");
            resultMap.put("playbacks", list);
            //播放等待音乐
//            resultMap.put("sound_file_dir", "/ddt/fs/sounds/cti/acd");
            resultMap.put("quickresponse", true);
            resultMap.put("log", "create succeed");
        } else if ("input".equals(method)) {
            String privatedata = (String) tokenMap.get("privatedata");
            log.info("privatedata ===>{}", privatedata);
            String input_type = (String) tokenMap.get("input_type");
            log.info("input_type ===>{}", input_type);
            String input_args = (String) tokenMap.get("input_args");
            log.info("input_args ===>{}", input_args);
            Long input_start_time = (Long) tokenMap.get("input_start_time");
            log.info("input_start_time ===>{}", input_start_time);
            Integer input_duration = (Integer) tokenMap.get("input_duration");
            log.info("input_duration ===>{}", input_duration);
            //机器人没放音 0  在放音有时间
            Integer play_progress = (Integer) tokenMap.get("play_progress");
            log.info("play_progress ===>{}", play_progress);


            if ("complete".equals(input_type)) {
                if (input_args.contains("hangup")) {
                    resultMap.put("action", "hangup");
                    resultMap.put("log", "挂机");
                } else {
                    resultMap.put("action", "cti_play_and_detect_speech");
                    String date = LocalDateTime.now().toString();
                    resultMap.put("argument", "'1' '1' '0' '0.8' '" + asrAddr + "' '120' '800' '5000' '20000' '' '' '" + appid + "' '1' '" + date + "' 'wav'");
                    resultMap.put("privatedata", "test");
                    resultMap.put("playbacks", Collections.singletonList("您可以继续向我提问"));
                    resultMap.put("quickresponse", true);
                    resultMap.put("log", "重新开始放音");
                }
            } else {
                String prefix = StrUtil.sub(input_args, 0, 1);
                String text = StrUtil.subSuf(input_args, 1);
                if ("S".equals(prefix)) {
                    if (!"stop".equals(privatedata)) {
                        if (play_progress > 0) {
                            resultMap.put("commands", Collections.singletonList("uuid_cti_play_and_detect_speech_break_play " + callid));
                            resultMap.put("privatedata", "stop");
                            resultMap.put("log", "停止放音，但是不停止ASR识别。模拟关键词打断");
                        }
                    }
                } else if ("F".equals(prefix)) {
                    if (text.contains("挂断")) {
                        resultMap.put("action", "hangup");
                        resultMap.put("privatedata", "test");
                        resultMap.put("playbacks", Collections.singletonList("谢谢您的提问，再见"));
                        resultMap.put("log", "挂机");
                    }
                    else{
                        if(0<play_progress&&text.length()>3||0==play_progress) {
                            String str = sendQuestion(text);
                            resultMap.put("action", "cti_play_and_detect_speech");
                            String date = LocalDateTime.now().toString();
                            resultMap.put("argument", "'0' '1' '0' '0.8' '" + asrAddr + "' '120' '800' '5000' '20000' '' '' '" + appid + "' '1' '" + date + "' 'wav'");
                            resultMap.put("privatedata", "test");
                            resultMap.put("playbacks", Arrays.asList(str));
                            resultMap.put("quickresponse", true);
                            resultMap.put("log", "播放识别结果");
                        }
                    }
                }
                if ("D".equals(prefix)) {
                    resultMap.put("action", "cti_play_and_detect_speech");
                    String date = LocalDateTime.now().toString();
                    resultMap.put("argument", "'0' '1' '0' '0.8' '" + asrAddr + "' '120' '800' '10000' '20000' '' '' '" + appid + "' '1' '" + date + "' 'wav'");
                    resultMap.put("privatedata", "test");
                    resultMap.put("dtmf_terminators", "#");
                    resultMap.put("playbacks", Arrays.asList("刚刚的按键内容是", text, "请继续按键测试吧,并以#号键结束"));
                    resultMap.put("log", "按键识别结果");
                } else {
                    resultMap.put("log", "no processing");
                }
            }
        } else if ("destory".equals(method)) {
            resultMap.put("log", "destory succeed");
        }
        return resultMap;
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
