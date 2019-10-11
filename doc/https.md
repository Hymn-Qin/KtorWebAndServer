## https 支持
1. 首先想办法弄到了一个备过案的域名（不然微信审不过去），绑定解析 IP 后，ssh 登录到远程机器

2. 在服务器上先进行安装操作，需要安装 certbot（不花钱）
   >  sudo apt install whois certbot
3. 安装完成后，就可以申请证书了
   >export DOMAIN=rarnu.com
   >$ export EMAIL=admin@rarnu.com
   >$ export PORT=80
   >$ export ALIAS=rarnu
   >$ sudo certbot certonly -n -d $DOMAIN --email "$EMAIL" --agree-tos --standalone --preferred-challenges http --http-01-port $PORT
这里的 DOMAIN 即是要申请证书的域名，注意必须是域名，IP 地址不可以。
后面的 EMAIL 是注册域名时填的邮箱地址（经过实际验证似乎任意邮箱都行，假的也行）
。再往后是服务的端口号，默认填80就可以了，如果填了别的，也会被重定向到80，
因此在申请证书时，千万记得不能开启 ktor 服务。最后的 ALIAS 是证书的别名，随便填写就好
4. 这个时候我们可以进入目录看到证书文件:cd /etc/letsencrypt/live/rarnu.com
5. 下面我们来把证书转换成 ktor 所需的 keystore.jks 文件
   > openssl pkcs12 -export -out /etc/letsencrypt/live/$DOMAIN/keystore.p12 -inkey /etc/letsencrypt/live/$DOMAIN/privkey.pem -in /etc/letsencrypt/live/$DOMAIN/fullchain.pem -name $ALIAS
   这个命令会让你设置证书的密码，设置完成后会生成一个 keystore.p12 文件，然后再把 p12 转为我们要的最终文件:
   keytool -importkeystore -alias $ALIAS -destkeystore /etc/letsencrypt/live/$DOMAIN/keystore.jks -srcstoretype PKCS12 -srckeystore /etc/letsencrypt/live/$DOMAIN/keystore.p12
   这个命令也会让你输入密码，这是 keystore 的密码，可以与 p12 一致或不一致，别忘了就好，完成后生成 keystore.jks，这个文件就是我们最终要用的了
6. 现在，打开 ktor 项目，在 application.conf 内加入相关的配置:

    application.conf 配置
    
    ktor {
    deployment {
        port = 80
        port = ${?PORT}
        sslPort = 443
        sslPort = ${?PORT_SSL}
    }
    security {
        ssl {
            keyStore = /etc/letsencrypt/live/rarnu.com/keystore.jks
            keyAlias = rarnu
            keyStorePassword = 123456
            privateKeyPassword = 123456
        }
    }
    application {
        modules = [ ... ]
    }
}

## 把域名分配到本地

1. 操作方式是在 /etc/hosts 里把域名配到本地: 
   >127.0.0.1  rarnu.com
   
2. 直接把服务器端的 keystore.jks 拷到本地的目录（这里指的是 /etc/letsencrypt/live/rarnu.com/）就可以本地调试了。
   >ssl {
   >    keyStore = /etc/letsencrypt/live/rarnu.com/keystore.jks
   >    keyAlias = rarnu
   >    keyStorePassword = 123456
   >    privateKeyPassword = 123456
   > }
### 自签名证书
1. 添加依赖
   >compile "io.ktor:ktor-network-tls:$ktor_version"
3. 在Application.kt中添加
   >fun main(args: Array<String>) { 
   > generateCertificate(File("keystore.jks")) 
   >}
4. 然后就可以把生成的 jks 配到项目里了，需要注意的是自签名证书拥有默认的 alias 和 password：
   >security {
   > ssl {
   >     keyStore = keystore.jks
   >     keyAlias = mykey
   >     keyStorePassword = changeit
   >     privateKeyPassword = changeit
   > }
   >}
