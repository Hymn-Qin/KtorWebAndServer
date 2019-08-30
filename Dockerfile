
# We select the base image from. Locally available or from https://hub.docker.com/
# 使用以 Alpine Linux 预构建的镜像作为基础镜像。也可以使用 OpenJDK registry 中的其他镜像。Alpine Linux 的好处是镜像非常小。
# 我们选择的也是只有 JRE 的镜像，因为我们并不需要在镜像中编译代码，只需要运行预编译的类。
FROM openjdk:8-jre-alpine

# We define the user we will use in this instance to prevent using root that even in a container, can be a security risk.
ENV APPLICATION_USER ktor

# Then we add the user, create the /app folder and give permissions to our user.
RUN adduser -D -g '' $APPLICATION_USER
RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

# Marks this container to use the specified $APPLICATION_USER
USER $APPLICATION_USER

# We copy the FAT Jar we built into the /app folder and sets that folder as the working directory.
COPY ./build/libs/my-application.jar /app/my-application.jar
WORKDIR /app

# 以上 将已打包的应用复制到 Docker 镜像中，并将工作目录设置为复制后的位置。

# 最后一行指示 Docker 使用 G1 GC、4G 内存以及已打包的应用来运行 java。
# We launch java to execute the jar, with good defauls intended for containers.
CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "my-application.jar"]

# 构建docker镜像
# gradle clean build
# 构建并标记镜像：
# docker build -t my-application .
# 启动镜像  并且响应 ctrl+c 推出
# docker run -m512M --cpus 2 -it -p 8087:8087 --rm my-application

# 通过这个命令，我们启动 Docker 进入前台模式。 它会等待服务器退出， 也会响应 Ctrl+C 来停止服务。
# -it 指示 Docker 分配一个终端（tty）来管理标准输出（stdout） 并响应中断键序列。
# 由于我们的服务器现在是在一个隔离的容器中运行，因此我们应该告诉 Docker 暴露一个端口，以便我们可以实际访问到服务器端口。
# -p 8080:8080 参数 告诉 Docker 将容器内部的 8087 端口发布为本机的 8087 端口。
# 因此，当告诉浏览器访问 localhost:8087 时，它会首先连向 Docker，然后为应用将其桥接到内部端口 8087。
# -m512M 调整内存并通过 --cpus 2 调整所暴露 cpu 数。
# 默认情况下，容器的文件系统在容器退出后仍然存在，所以我们提供 --rm 选项以防垃圾堆积。