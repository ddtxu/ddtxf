# ddtxf 
实现目标 ：希望能够以语音的形式向提问机器人提出问题，机器人语音回答提的问题

1.准备工作
       1. 安装了FreeSWITCH   http://www.ddrj.com/callcenter/userguide.html
       2. 安装了(mod_cti基于FreeSWITCH)-语音识别（asr）接口  http://www.ddrj.com/callcenter/asr.html
       3. 下载ccAdmin和sipphone(方便测试)  http://www.ddrj.com/callcenter/gui.html  http://www.ddrj.com/sipphone/index.html
       4. 申请了免费的星火大模型套餐，获取到相关key 和相关信息，代码里面要填写的  https://xinghuo.xfyun.cn/sparkapi?scr=price
2.java 后端接口说明
        1. 项目说明
                这个项目是使用java 代码实现与讯飞大模型对接，实现机器人问答功能。
3. 下载代码后请将在火星大模型获得的key 等相关内容填写到 application.yml文件中
4. 可围观CSDN https://blog.csdn.net/qq_52528295/article/details/133741976?spm=1001.2014.3001.5502  里有详细介绍和解释
