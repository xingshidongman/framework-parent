\<blueprint xmlns:jpa="http://aries.apache.org/xmlns/jpa/v2.0.0"
     xmlns:tx="http://aries.apache.org/xmlns/transactions/v1.2.0"
     xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0">
     <jpa:enable/>
     <tx:enable-annotations/>

    \<reference id="shiroService" interface="com.kalix.framework.core.api.security.IShiroService"/>
    \<reference id="<pomName>BeanDao" interface="<packageName>.api.dao.I<beanName>BeanDao"/>

    \<bean id="<pomName>BeanServiceImpl" class="<packageName>.biz.<beanName>BeanServiceImpl">
        \<property name="dao" ref="<pomName>BeanDao"/>
        \<property name="shiroService" ref="shiroService"/>
    \</bean>

    \<service id="<pomName>BeanService" interface="<packageName>.api.biz.I<beanName>BeanService"
             ref="<pomName>BeanServiceImpl">
    \</service>
\</blueprint>

