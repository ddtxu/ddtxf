package com.zhulang.xfxhsimple.controller;

import cn.hutool.core.util.StrUtil;
import com.zhulang.xfxhsimple.component.XfXhStreamClient;
import com.zhulang.xfxhsimple.config.XfXhConfig;
import com.zhulang.xfxhsimple.dto.MsgDTO;
import com.zhulang.xfxhsimple.listener.XfXhWebSocketListener;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
@RestController
@RequestMapping("/test")
@Slf4j
public class DDTestController {

    @Resource
    private XfXhStreamClient xfXhStreamClient;

    @Resource
    private XfXhConfig xfXhConfig;

    @PostMapping("/flow")
    public Map<String, Object> flow(@RequestBody Map<String, Object> tokenMap) throws Exception {
        String asrAddr = "127.0.0.1:9988";
        String ttsAddr = "ws://129.211.24.206:9989/tts";

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
            ttsMap.put("ttsvoicename", "");
            ttsMap.put("ttsconfig", "");
            ttsMap.put("ttsengine", "");
            ttsMap.put("ttsvolume", 0);
            ttsMap.put("ttsspeechrate", 0);
            ttsMap.put("ttspitchrate", 0);

            resultMap.put("tts", ttsMap);
            resultMap.put("privatedata", "test");
            List<String> list = Arrays.asList("欢迎进入测试程序，被叫号码是", "15307", "请继续说话测试吧", "等待音乐.wav");
            resultMap.put("playbacks", list);
            resultMap.put("sound_file_dir", "/ddt/fs/sounds/cti/acd");
            resultMap.put("pre_tts_text", Arrays.asList("徐先生", "2023年10月10日"));
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
            Integer play_progress = (Integer) tokenMap.get("play_progress");
            log.info("play_progress ===>{}", play_progress);
            if ("complete".equals(input_type)) {
//				String inputArgSub = input_args.substring(0, 6);
                if (input_args.contains("hangup")) {
                    resultMap.put("action", "hangup");
                    resultMap.put("log", "挂机");
                } else if (input_args.contains("record")) {
                    String recordfile = StrUtil.subWithLength(input_args, 7, input_args.length() - 8);
                    resultMap.put("action", "cti_play_and_detect_speech");
                    String date = LocalDateTime.now().toString();
                    resultMap.put("argument", "'1' '1' '0' '0.8' '" + asrAddr + "' '120' '800' '5000' '20000' '' '' " + appid + " '1' '" + date + "' 'wav'");
                    resultMap.put("privatedata", "test");
                    resultMap.put("playbacks", Arrays.asList("刚刚的录音内容是", recordfile, "请继续说话，可以说关键词，人工，转接，暂停，停止，分机来测试"));
                    resultMap.put("quickresponse", true);
                    resultMap.put("log", "播放录音");
                } else {
                    resultMap.put("action", "cti_play_and_detect_speech");
                    String date = LocalDateTime.now().toString();
                    resultMap.put("argument", "'1' '1' '0' '0.8' '" + asrAddr + "' '120' '800' '5000' '20000' '' '' '" + appid + "' '1' '" + date + "' 'wav'");
                    resultMap.put("privatedata", "test");
                    resultMap.put("playbacks", Collections.singletonList("动作执行完成，这里必须放音，请继续说话，可以继续提问"));
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
                        resultMap.put("playbacks", Collections.singletonList("谢谢你的测试，再见"));
                        resultMap.put("log", "挂机");
                    }
                    else{
                        //根据用户语音提问讯飞大模型
                        String str = sendQuestion(text);
                        resultMap.put("action", "cti_play_and_detect_speech");
                        String date = LocalDateTime.now().toString();
                        resultMap.put("argument", "'1' '1' '0' '0.8' '" + asrAddr + "' '120' '800' '5000' '20000' '' '' '" + appid + "' '1' '" + date + "' 'wav'");
                        resultMap.put("privatedata", "test");
                        //回答的问题转语音
                        resultMap.put("playbacks", Collections.singletonList(str));
                        resultMap.put("quickresponse", true);
                        resultMap.put("log", "播放识别结果");
                    }
                }
                if ("D".equals(prefix)) {
                    resultMap.put("action", "cti_play_and_detect_speech");
                    String date = LocalDateTime.now().toString();
                    resultMap.put("argument", "'1' '1' '0' '0.8' '" + asrAddr + "' '120' '800' '10000' '20000' '' '' '" + appid + "' '1' '" + date + "' 'wav'");
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

    private StringBuilder respons(String str){
        str.replaceAll("\\s+", "");
        String[] parts = str.split("[，。!]");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append("\"").append(parts[i]).append("\"");
            if (i != parts.length - 1) {
                sb.append(","); }
        }
        return sb;

    }


    private String sendQuestion( String question) {
        // 如果是无效字符串，则不对大模型进行请求
        if (StrUtil.isBlank(question)) {
            return "无效问题，请重新输入";
        }
        // 获取连接令牌
        if (!xfXhStreamClient.operateToken(XfXhStreamClient.GET_TOKEN_STATUS)) {
            return "当前大模型连接数过多，请稍后再试";
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
            return "系统内部错误，请联系管理员";
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
                return "大模型响应超时，请联系管理员";
            }
            // 响应大模型的答案
            return listener.getAnswer().toString();
        } catch (InterruptedException e) {
            log.error("错误：" + e.getMessage());
            return "系统内部错误，请联系管理员";
        } finally {
            // 关闭 websocket 连接
            webSocket.close(1000, "");
            // 归还令牌
            xfXhStreamClient.operateToken(XfXhStreamClient.BACK_TOKEN_STATUS);
        }
    }

}
