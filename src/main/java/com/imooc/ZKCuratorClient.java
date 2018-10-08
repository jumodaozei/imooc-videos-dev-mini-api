package com.imooc;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class ZKCuratorClient {
	
	private CuratorFramework client = null;
	final static Logger log = LoggerFactory.getLogger(ZKCuratorClient.class);
	
	private static final String ZOOKEEPER_SERVER = "192.168.13.236:2181";
	
	public void init() {
		if(client != null) {
			return;
		}
		
		//重试策略
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000,5);
		
		//创建客户端
		client = CuratorFrameworkFactory.builder().connectString(ZOOKEEPER_SERVER)
				.sessionTimeoutMs(10000).retryPolicy(retryPolicy).namespace("admin").build();
		
		//启动客户端
		client.start();
		
		try {
			String testnode = new String(client.getData().forPath("/bgm/180930B7P460M5AW"));
			log.info("测试节点数据:{}",testnode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
