/*******************************************************************************
 * Copyright 2018 IIT-CNR
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package it.cnr.iit.ucs.pipldap;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Subscriber timer for the PIPReader. It's in charge of performing the task of
 * refreshing periodically the value of a certain attribute.
 *
 * @author Antonio La Marra, Alessandro Rosetti
 */
final class PIPLdapSubscriberTimer extends TimerTask {
	private Timer timer;
	PIPLdap pip;

	private static final long DEFAULT_RATE = 1L * 6000000;
	private long rate = DEFAULT_RATE;

	PIPLdapSubscriberTimer(PIPLdap pip) {
		this.timer = new Timer();
		this.pip = pip;
	}

	@Override
	public void run() {
		pip.checkSubscriptions();
	}

	public void start() {
		timer.scheduleAtFixedRate(this, 0, rate);
	}

	public long getRate() {
		return rate;
	}

	public void setRate(long rate) {
		if (rate <= 0) {
			this.rate = DEFAULT_RATE;
		}
		this.rate = rate;
	}

}
