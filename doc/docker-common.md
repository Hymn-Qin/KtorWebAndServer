**docker 常用命令**

| 命令                                                                | 含义                                                                                 | 备注                                      |
|:-------------------------------------------------------------------|:--------------------------------------------------------------------------------------|:----------------------------------------|
| sudo service docker start                                          | 启动docker                                                                             |                                         |
| sudo service docker stop                                           | 停止docker                                                                             |                                         |
| sudo docker --version                                              | 查看docker版本                                                                          |                                         |
| sudo docker ps -a                                                  | 列出所有容器                                                                             | 不加 -a 仅列出正在运行的，像退出了的或者仅仅只是创建了的就不列出来     |
| sudo docker run -d -p 8800:80 --name name/id  nginx                | 运行指定的镜像                                                                            | 宿主主机端口：容器内部端口                           |
|                                                                    | -d  后台运行                                                                            |                                         |
|                                                                    | -p 8800:80 是指定对外暴露的端口                                                           | 容器内部用80 对应外部宿主主机的的8800  代理一样            |
|                                                                    | --name指定容器的名字  最后的nginx 代码要运行的镜像名字                                        | 有tag的加上tag 如 nginx：xxx  默认为latest       |
| 　　                                                                | 然后访问宿主主机地址+8800端口                                                             |                                         |
| sudo docker run -d --privileged=true -p 83:80 --name nginx83 nginx | 提升权限                                                                              |                                         |
| sudo docker exec -it 54d26bbce3d6 /bin/bash                        | 进入容器内部                                                                            | 容器id或容器名字 进入之后就和操作新的系统一样，操作完成之后输入exit退出 |
| sudo docker inspect name/id                                        | 获取镜像信息详情                                                                         | 比如安装了mysql后需要获取镜像内部的IP，端口等              |
| sudo docker inspect name/id \|grep IPAddress                       | 通过grep检索需要的项目                                                                   | |
| sudo docker commit name/id xiaochangwei/nginx:v1.0                 | nginx_xiao 表示我们刚修改的容器名字或者id xiaochangwei/nginx:v1.0 为保存的镜像名字 ：后面为tag  | 进入容器内部并修改了东西后，生成新的镜像供下次直接使用             |
| sudo docker ps -a                                                  | 查看所在容器                                                                            | |
| sudo docker start/stop/restart name/id                             | 来启动、停止、重启指定的容器                                                               | |
| sudo docker rmi xxx                                                | 来删除指定的镜像                                                                        | 镜像存在依赖关系，先删除最下层，最后删除顶层，建议根据镜像名字来删除      |
| docker pull xiaochangwei/nginx:v1.0                                |获取并使用这个镜像                                                                      |镜像名里面包含了注册的账户名|

___

**commit的镜像仅仅是保存在本地的，如果要提交到网络上供其他人pull 使用呢？** 

如 https://cloud.docker.com/  
1. 在https://cloud.docker.com/上注册一个账号 
2. 提交本地镜像到https://cloud.docker.com/上去  
别人就可以通过docker pull xiaochangwei/nginx:v1.0 来获取并使用这个镜像了，镜像名里面包含了注册的账户名，这里需要一致，否则无法push

**权限**
1. sudo groupadd docker  创建用户组
2. sudo gpasswd -a ${USER} docker  当前用户添加到用户的组
3. newgrp  docker  切换用户组
4. sudo service docker restart   重启
