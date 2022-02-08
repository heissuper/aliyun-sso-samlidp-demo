# SAML IDP DEMO
## 接口说明
1. /saml/metadata 用于IDP SAML 元数据的生成, 需要将该元数据提供给阿里云用户SSO用于身份接入
2. /saml/login    用于用户登陆
3. /saml/list     用于列出应用列表
4. /saml/sso      用于接受 SP AuthnRequest(SAML 请求)的URL,暂时没用

## 项目配置说明
##### 1.生成自己用于SAML加签的密钥与元数据所使用的证书
* (1) 生成RSA私钥 openssl genrsa -aes128 -passout pass:${password} -out rsa_aes_private.key 2048 
* (2) 生成证书请求文件 openssl req -new -key rsa_aes_private.key -passin pass:${password} -out client.pem (可以添加上-days 365参数延长证书有效期)
* (3) 生成最终的SAML使用的x509证书 openssl req -x509 -key rsa_aes_private.key -in client.pem -out client.pem
* (4)通过以上2步之后会获取2个文件,一个是rsa_aes_private.key 这个是密钥,密钥自己妥善保存,不可以让任何人知道,如果泄露则所在的租户账户都有风险. 证书用于生成SAML元数据是可以对外公布的.

##### 2. 用上面生成的rsa_aes_private.key文件与client.pem文件替换 DEMO 中的 resources文件夹下面的rsa_aes_private.key与client.pem文件
##### 3. 打开application.properties文件,修改iot.saml.host,iot.saml.port,iot.saml.companyId与iot.key.password(生成证书使用的密码,即上文的${password})参数,并通过SamldemoApplication启动
##### 4. 访问http://${iot.saml.host}:${iot.saml.port}/saml/metadata 即可获取元数据
##### 5. 生成的元数据及提供相应的信息之后,https://ram.console.aliyun.com/providers SSO管理->用户SSO中上传,即可完成接入流程.

## 其他  

1. 生成X509证书命令的操作部署见`X509CertGenTest`类123