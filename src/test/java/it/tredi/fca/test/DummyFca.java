package it.tredi.fca.test;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.tredi.fca.Fca;
import it.tredi.fca.entity.FcsRequest;

public class DummyFca extends Fca {

	private static final Logger logger = LogManager.getLogger(DummyFca.class.getName());
	
	private static final int NUM_REQ_FCS = 100;
	
	public static void main(String[] args) {
		try {
			DummyFca dummyFca = new DummyFca();
			dummyFca.run();
			
			if (logger.isInfoEnabled())
				logger.info("DummyFca.main(): shutdown...");
			System.exit(0);
		}
		catch(Exception e) {
			logger.error("DummyFca.main(): got exception... " + e.getMessage(), e);
			System.exit(1);
		}
	}
	
	public DummyFca() throws Exception {
		super();
		
		if (logger.isInfoEnabled())
			logger.info("DummyFca... specific configuration...");
	}

	@Override
	public List<FcsRequest> loadFcsPendingRequests() throws Exception {
		List<FcsRequest> reqs = new ArrayList<FcsRequest>();
		for (int i=0; i<NUM_REQ_FCS; i++)
			reqs.add(new FcsRequest("AAA"+String.valueOf(i)));
		return reqs;
	}

	@Override
	public void onRunException(Exception e) {
		if (logger.isInfoEnabled())
			logger.info("DummyFca... run exception...");
	}

	@Override
	public void onRunFinally() {
		if (logger.isInfoEnabled())
			logger.info("DummyFca... run finally...");
	}

	@Override
	public String getAppVersion() {
		return "1.0.0-DUMMY";
	}

	@Override
	public String getAppBuildDate() {
		return "NOW";
	}

}
