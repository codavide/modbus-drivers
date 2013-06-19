/*
 * Dog 2.0 - Modbus Device Driver
 * 
 * Copyright [2012] 
 * [Dario Bonino (dario.bonino@polito.it), Politecnico di Torino] 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License. 
 */
package it.polito.elite.domotics.dog2.modbusthreephaseelectricitymeterdriver;

import it.polito.elite.domotics.dog2.doglibrary.devicecategory.ControllableDevice;
import it.polito.elite.domotics.dog2.doglibrary.util.DogLogInstance;
import it.polito.elite.domotics.dog2.modbusnetworkdriver.ModbusDriver;
import it.polito.elite.domotics.dog2.modbusnetworkdriver.info.CmdNotificationInfo;
import it.polito.elite.domotics.dog2.modbusnetworkdriver.info.ModbusRegisterInfo;
import it.polito.elite.domotics.dog2.modbusnetworkdriver.interfaces.ModbusNetwork;
import it.polito.elite.domotics.model.DeviceStatus;
import it.polito.elite.domotics.model.devicecategory.ThreePhaseElectricityMeter;
import it.polito.elite.domotics.model.notification.FrequencyMeasurementNotification;
import it.polito.elite.domotics.model.notification.SinglePhaseActiveEnergyMeasurementNotification;
import it.polito.elite.domotics.model.notification.SinglePhaseReactiveEnergyMeasurementNotification;
import it.polito.elite.domotics.model.notification.ThreePhaseActivePowerMeasurementNotification;
import it.polito.elite.domotics.model.notification.ThreePhaseApparentPowerMeasurementNotification;
import it.polito.elite.domotics.model.notification.ThreePhaseCurrentMeasurementNotification;
import it.polito.elite.domotics.model.notification.ThreePhaseLNVoltageMeasurementNotification;
import it.polito.elite.domotics.model.notification.ThreePhaseReactivePowerMeasurementNotification;
import it.polito.elite.domotics.model.state.FrequencyMeasurementState;
import it.polito.elite.domotics.model.state.PowerFactorMeasurementState;
import it.polito.elite.domotics.model.state.SinglePhaseActiveEnergyState;
import it.polito.elite.domotics.model.state.SinglePhaseReactiveEnergyState;
import it.polito.elite.domotics.model.state.State;
import it.polito.elite.domotics.model.state.ThreePhaseActivePowerMeasurementState;
import it.polito.elite.domotics.model.state.ThreePhaseApparentPowerMeasurementState;
import it.polito.elite.domotics.model.state.ThreePhaseCurrentState;
import it.polito.elite.domotics.model.state.ThreePhaseReactivePowerMeasurementState;
import it.polito.elite.domotics.model.state.ThreePhaseVoltageState;
import it.polito.elite.domotics.model.statevalue.ActiveEnergyStateValue;
import it.polito.elite.domotics.model.statevalue.ActivePowerStateValue;
import it.polito.elite.domotics.model.statevalue.ApparentPowerStateValue;
import it.polito.elite.domotics.model.statevalue.CurrentStateValue;
import it.polito.elite.domotics.model.statevalue.FrequencyStateValue;
import it.polito.elite.domotics.model.statevalue.PowerFactorStateValue;
import it.polito.elite.domotics.model.statevalue.ReactiveEnergyStateValue;
import it.polito.elite.domotics.model.statevalue.ReactivePowerStateValue;
import it.polito.elite.domotics.model.statevalue.StateValue;
import it.polito.elite.domotics.model.statevalue.VoltageStateValue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.quantity.Power;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * @author bonino
 * 
 */
public class ModbusThreePhaseElectricityMeterDriverInstance extends ModbusDriver implements ThreePhaseElectricityMeter
{
	// the class logger
	private LogService logger;
	
	public ModbusThreePhaseElectricityMeterDriverInstance(ModbusNetwork network, ControllableDevice device,
			String gatewayAddress, String gatewayPort, String gatewayProtocol, BundleContext context)
	{
		super(network, device, gatewayAddress, gatewayPort, gatewayProtocol);
		
		// create a logger
		this.logger = new DogLogInstance(context);
		
		// TODO: get the initial state of the device....(states can be updated
		// by reading notification group addresses)
		this.initializeStates();
	}
	
	@Override
	public void detachDriver(String deviceID)
	{
		// nothing to do by now... will be handled in the future... may be...
	}
	
	@Override
	public Measure<?, ?> getReactiveEnergyValue()
	{
		return (Measure<?, ?>) this.currentState.getState(SinglePhaseReactiveEnergyState.class.getName())
				.getCurrentStateValue()[0].getValue();
	}
	
	@Override
	public Measure<?, ?> getReactivePower(String phaseID)
	{
		return this.getThreePhaseStateValue(ThreePhaseReactivePowerMeasurementState.class.getName(), phaseID);
	}
	
	@Override
	public Measure<?, ?> getFrequency()
	{
		return (Measure<?, ?>) this.currentState.getState(FrequencyMeasurementState.class.getName())
				.getCurrentStateValue()[0].getValue();
	}
	
	@Override
	public Measure<?, ?> getPowerFactor()
	{
		return (Measure<?, ?>) this.currentState.getState(PowerFactorMeasurementState.class.getName())
				.getCurrentStateValue()[0].getValue();
	}
	
	@Override
	public Measure<?, ?> getActiveEnergyValue()
	{
		return (Measure<?, ?>) this.currentState.getState(SinglePhaseActiveEnergyState.class.getName())
				.getCurrentStateValue()[0].getValue();
	}
	
	@Override
	public Measure<?, ?> getLNVoltageValue(String phaseID)
	{
		return this.getThreePhaseStateValue(ThreePhaseVoltageState.class.getName(), phaseID);
	}
	
	@Override
	public Measure<?, ?> getLLVoltageValue(String phaseID1, String phaseID2)
	{
		// TODO: fix this....
		String phaseID = phaseID1 + phaseID2.substring(1);
		
		return this.getThreePhaseStateValue(ThreePhaseVoltageState.class.getName() + "LL", phaseID);
	}
	
	@Override
	public Measure<?, ?> getElectricCurrentValue(String phaseID)
	{
		return this.getThreePhaseStateValue(ThreePhaseCurrentState.class.getName(), phaseID);
	}
	
	@Override
	public Measure<?, ?> getApparentPower(String phaseID)
	{
		return this.getThreePhaseStateValue(ThreePhaseApparentPowerMeasurementState.class.getName(), phaseID);
	}
	
	@Override
	public Measure<?, ?> getActivePower(String phaseID)
	{
		return this.getThreePhaseStateValue(ThreePhaseActivePowerMeasurementState.class.getName(), phaseID);
	}
	
	@Override
	public DeviceStatus getState()
	{
		return this.currentState;
	}
	
	@Override
	public void notifyNewFrequencyValue(Measure<?, ?> frequency)
	{
		// update the state
		((FrequencyMeasurementState) this.currentState.getState(FrequencyMeasurementState.class.getName()))
				.getCurrentStateValue()[0].setValue(frequency);
		
		// notify the new measure
		((ThreePhaseElectricityMeter) this.device).notifyNewFrequencyValue(frequency);
		
	}
	
	@Override
	public void notifyStateChanged(State newState)
	{
		// probably unused...
		((ThreePhaseElectricityMeter) this.device).notifyStateChanged(newState);
		
	}
	
	@Override
	public void notifyNewReactivePowerValue(String phaseID, Measure<?, ?> value)
	{
		// update the state....
		this.updateThreePhaseStateValue(ThreePhaseReactivePowerMeasurementState.class.getName(), phaseID, value);
		
		((ThreePhaseElectricityMeter) this.device).notifyNewReactivePowerValue(phaseID, value);
		
	}
	
	@Override
	public void notifyNewReactiveEnergyValue(Measure<?, ?> value)
	{
		// update the state
		this.currentState.getState(SinglePhaseReactiveEnergyState.class.getName()).getCurrentStateValue()[0]
				.setValue(value);
		
		// notify the new measure
		((ThreePhaseElectricityMeter) this.device).notifyNewReactiveEnergyValue(value);
		
	}
	
	@Override
	public void notifyNewActiveEnergyValue(Measure<?, ?> value)
	{
		// update the state
		((SinglePhaseActiveEnergyState) this.currentState.getState(SinglePhaseActiveEnergyState.class.getName()))
				.getCurrentStateValue()[0].setValue(value);
		
		// notify the new measure
		((ThreePhaseElectricityMeter) this.device).notifyNewActiveEnergyValue(value);
	}
	
	@Override
	public void notifyNewPhaseNeutralVoltageValue(String phaseID, Measure<?, ?> value)
	{
		// update the state....
		this.updateThreePhaseStateValue(ThreePhaseVoltageState.class.getName(), phaseID, value);
		
		((ThreePhaseElectricityMeter) this.device).notifyNewPhaseNeutralVoltageValue(phaseID, value);
		
	}
	
	@Override
	public void notifyNewPhasePhaseVoltageValue(String phaseID, Measure<?, ?> value)
	{
		
		this.updateThreePhaseStateValue(ThreePhaseVoltageState.class.getName() + "LL", phaseID, value);
		
		((ThreePhaseElectricityMeter) this.device).notifyNewPhasePhaseVoltageValue(phaseID, value);
		
	}
	
	@Override
	public void notifyNewApparentPowerValue(String phaseID, Measure<?, ?> value)
	{
		// update the state....
		
		this.updateThreePhaseStateValue(ThreePhaseApparentPowerMeasurementState.class.getName(), phaseID, value);
		
		((ThreePhaseElectricityMeter) this.device).notifyNewApparentPowerValue(phaseID, value);
		
	}
	
	@Override
	public void notifyNewPowerFactorValue(Measure<?, ?> powerFactor)
	{
		// update the state
		this.currentState.getState(PowerFactorMeasurementState.class.getName()).getCurrentStateValue()[0]
				.setValue(powerFactor);
		
		// notify the new measure
		((ThreePhaseElectricityMeter) this.device).notifyNewPowerFactorValue(powerFactor);
		
	}
	
	@Override
	public void notifyNewActivePowerValue(String phaseID, Measure<?, ?> value)
	{
		// update the state....
		this.updateThreePhaseStateValue(ThreePhaseActivePowerMeasurementState.class.getName(), phaseID, value);
		
		((ThreePhaseElectricityMeter) this.device).notifyNewActivePowerValue(phaseID, value);
	}
	
	@Override
	public void notifyNewCurrentValue(String phaseID, Measure<?, ?> value)
	{
		// update the state....
		this.updateThreePhaseStateValue(ThreePhaseCurrentState.class.getName(), phaseID, value);
		
		((ThreePhaseElectricityMeter) this.device).notifyNewCurrentValue(phaseID, value);
		
	}
	
	@Override
	public void newMessageFromHouse(ModbusRegisterInfo register, String value)
	{
		if (value != null)
		{
			// gets the corresponding notification set...
			Set<CmdNotificationInfo> notificationInfos = this.register2Notification.get(register);
			
			// handle the notifications
			for (CmdNotificationInfo notificationInfo : notificationInfos)
			{
				// black magic here...
				String notificationName = notificationInfo.getName();
				
				// get the hypothetical class method name
				String notifyMethod = "notify" + Character.toUpperCase(notificationName.charAt(0))
						+ notificationName.substring(1);
				
				// search the method and execute it
				try
				{
					// log notification
					this.logger.log(LogService.LOG_DEBUG, ModbusThreePhaseElectricityMeterDriver.logId + "Device: "
							+ this.device.getDeviceId() + " is notifying " + notificationName + " value:"
							+ register.getXlator().getValue());
					// get the method
					if (notificationInfo.getParameters().containsKey("phaseID"))
					{
						Method notify = ModbusThreePhaseElectricityMeterDriverInstance.class.getDeclaredMethod(
								notifyMethod, String.class, Measure.class);
						// invoke the method
						notify.invoke(this, notificationInfo.getParameters().get("phaseID"),
								DecimalMeasure.valueOf(register.getXlator().getValue()));
					}
					else
					{
						Method notify = ModbusThreePhaseElectricityMeterDriverInstance.class.getDeclaredMethod(
								notifyMethod, Measure.class);
						// invoke the method
						notify.invoke(this, DecimalMeasure.valueOf(register.getXlator().getValue()));
					}
				}
				catch (Exception e)
				{
					// log the error
					this.logger.log(LogService.LOG_WARNING, ModbusThreePhaseElectricityMeterDriver.logId
							+ "Unable to find a suitable notification method for the datapoint: " + register + ":\n"
							+ e);
				}
				
			}
		}
		
	}
	
	@Override
	protected void specificConfiguration()
	{
		// prepare the device state map
		this.currentState = new DeviceStatus(device.getDeviceId());
		
	}
	
	private void initializeStates()
	{
		// Since this driver handles the device metering according to a well
		// defined interface, we can get the unit of measure from all the
		// notifications handled by this device except from state notifications
		// and fall back to WattHour/VarHour if the
		// procedure fails...
		
		// create the var and va units
		Unit<Power> VAR = SI.WATT.alternate("var");
		VAR.alternate("Var");
		Unit<Power> VA = SI.WATT.alternate("VA");
		
		// add unit of measure aliases (to fix notation problems...)
		UnitFormat uf = UnitFormat.getInstance();
		uf.alias(SI.WATT.times(NonSI.HOUR), "Wh");
		uf.label(SI.KILO(SI.WATT.times(NonSI.HOUR)), "kWh");
		uf.alias(VAR.times(NonSI.HOUR), "Varh");
		uf.label(SI.KILO(VAR.times(NonSI.HOUR)), "kVarh");
		uf.label(SI.KILO(VA), "kVA");
		
		String activeEnergyUOM = SI.WATT.times(NonSI.HOUR).toString();
		String reactiveEnergyUOM = VAR.times(NonSI.HOUR).toString();
		String activePowerUOM = SI.WATT.toString();
		String reactivePowerUOM = VAR.toString();
		String frequencyUOM = SI.HERTZ.toString();
		String currentUOM = SI.AMPERE.toString();
		String apparentPowerUOM = VA.toString();
		String voltageUOM = SI.VOLT.toString();
		
		// search the energy unit of measures declared in the device
		// configuration
		for (ModbusRegisterInfo register : this.register2Notification.keySet())
		{
			Set<CmdNotificationInfo> notificationInfos = this.register2Notification.get(register);
			
			for (CmdNotificationInfo notificationInfo : notificationInfos)
			{
				
				if (notificationInfo.getName().equalsIgnoreCase(
						SinglePhaseActiveEnergyMeasurementNotification.notificationName))
				{
					activeEnergyUOM = register.getXlator().getUnitOfMeasure();
				}
				else if (notificationInfo.getName().equalsIgnoreCase(
						SinglePhaseReactiveEnergyMeasurementNotification.notificationName))
				{
					reactiveEnergyUOM = register.getXlator().getUnitOfMeasure();
				}
				else if (notificationInfo.getName().equalsIgnoreCase(
						ThreePhaseReactivePowerMeasurementNotification.notificationName))
				{
					reactivePowerUOM = register.getXlator().getUnitOfMeasure();
				}
				else if (notificationInfo.getName().equalsIgnoreCase(
						ThreePhaseActivePowerMeasurementNotification.notificationName))
				{
					activePowerUOM = register.getXlator().getUnitOfMeasure();
				}
				else if (notificationInfo.getName().equalsIgnoreCase(FrequencyMeasurementNotification.notificationName))
				{
					frequencyUOM = register.getXlator().getUnitOfMeasure();
				}
				else if (notificationInfo.getName().equalsIgnoreCase(
						ThreePhaseCurrentMeasurementNotification.notificationName))
				{
					currentUOM = register.getXlator().getUnitOfMeasure();
				}
				else if (notificationInfo.getName().equalsIgnoreCase(
						ThreePhaseApparentPowerMeasurementNotification.notificationName))
				{
					apparentPowerUOM = register.getXlator().getUnitOfMeasure();
				}
				else if (notificationInfo.getName().equalsIgnoreCase(
						ThreePhaseLNVoltageMeasurementNotification.notificationName))
				{
					voltageUOM = register.getXlator().getUnitOfMeasure();
				}
				/*
				 * else if (this.register2Notification.get(register).contains(
				 * ThreePhaseLLVoltageMeasurementNotification.notificationName))
				 * { frequencyUOM = register.getXlator().getUnitOfMeasure(); }
				 */
			}
		}
		
		// create all the states
		
		// ------------ Three Phase Active Power
		// --------------------------------
		ActivePowerStateValue activePowerStateL1 = new ActivePowerStateValue();
		activePowerStateL1.setFeature("phaseID", "L1");
		activePowerStateL1.setValue(DecimalMeasure.valueOf("0 " + activePowerUOM));
		
		ActivePowerStateValue activePowerStateL2 = new ActivePowerStateValue();
		activePowerStateL2.setFeature("phaseID", "L2");
		activePowerStateL2.setValue(DecimalMeasure.valueOf("0 " + activePowerUOM));
		
		ActivePowerStateValue activePowerStateL3 = new ActivePowerStateValue();
		activePowerStateL3.setFeature("phaseID", "L3");
		activePowerStateL3.setValue(DecimalMeasure.valueOf("0 " + activePowerUOM));
		
		this.currentState.setState(ThreePhaseActivePowerMeasurementState.class.getName(),
				new ThreePhaseActivePowerMeasurementState(activePowerStateL1, activePowerStateL2, activePowerStateL3));
		// --------------------------------------------------------------------------
		
		// ------------- Frequency -----------------------------------------
		FrequencyStateValue frequencyStateValue = new FrequencyStateValue();
		frequencyStateValue.setValue(DecimalMeasure.valueOf("0 " + frequencyUOM));
		this.currentState.setState(FrequencyMeasurementState.class.getName(), new FrequencyMeasurementState(
				frequencyStateValue));
		// -----------------------------------------------------------------
		
		// -------------- SinglePhaseActivePower ---------------------------
		ActiveEnergyStateValue activeEnergyStateValue = new ActiveEnergyStateValue();
		activeEnergyStateValue.setValue(DecimalMeasure.valueOf("0 " + activeEnergyUOM));
		this.currentState.setState(SinglePhaseActiveEnergyState.class.getName(), new SinglePhaseActiveEnergyState(
				activeEnergyStateValue));
		// ------------------------------------------------------------------
		
		// -------------- ThreePhaseCurrent ---------------------------------
		CurrentStateValue currentStateL1 = new CurrentStateValue();
		currentStateL1.setFeature("phaseID", "L1");
		currentStateL1.setValue(DecimalMeasure.valueOf("0 " + currentUOM));
		
		CurrentStateValue currentStateL2 = new CurrentStateValue();
		currentStateL2.setFeature("phaseID", "L2");
		currentStateL2.setValue(DecimalMeasure.valueOf("0 " + currentUOM));
		
		CurrentStateValue currentStateL3 = new CurrentStateValue();
		currentStateL3.setFeature("phaseID", "L3");
		currentStateL3.setValue(DecimalMeasure.valueOf("0 " + currentUOM));
		
		this.currentState.setState(ThreePhaseCurrentState.class.getName(), new ThreePhaseCurrentState(currentStateL1,
				currentStateL2, currentStateL3));
		// -------------------------------------------------------------------
		
		// ------------- ThreePhaseReactivePower -----------------------------
		ReactivePowerStateValue reactivePowerStateL1 = new ReactivePowerStateValue();
		reactivePowerStateL1.setFeature("phaseID", "L1");
		reactivePowerStateL1.setValue(DecimalMeasure.valueOf("0 " + reactivePowerUOM));
		
		ReactivePowerStateValue reactivePowerStateL2 = new ReactivePowerStateValue();
		reactivePowerStateL2.setFeature("phaseID", "L2");
		reactivePowerStateL2.setValue(DecimalMeasure.valueOf("0 " + reactivePowerUOM));
		
		ReactivePowerStateValue reactivePowerStateL3 = new ReactivePowerStateValue();
		reactivePowerStateL3.setFeature("phaseID", "L3");
		reactivePowerStateL3.setValue(DecimalMeasure.valueOf("0 " + reactivePowerUOM));
		
		this.currentState.setState(ThreePhaseReactivePowerMeasurementState.class.getName(),
				new ThreePhaseReactivePowerMeasurementState(reactivePowerStateL1, reactivePowerStateL2,
						reactivePowerStateL3));
		// --------------------------------------------------------------------
		
		// ---------------------- Power Factor --------------------------------
		PowerFactorStateValue powerFactorStateValue = new PowerFactorStateValue();
		powerFactorStateValue.setValue(DecimalMeasure.valueOf("0 " + Unit.ONE));
		
		this.currentState.setState(PowerFactorMeasurementState.class.getCanonicalName(),
				new PowerFactorMeasurementState(powerFactorStateValue));
		// --------------------------------------------------------------------
		
		// ---------------------- ThreePhaseApparentPower ---------------------
		ApparentPowerStateValue apparentPowerStateL1 = new ApparentPowerStateValue();
		apparentPowerStateL1.setFeature("phaseID", "L1");
		apparentPowerStateL1.setValue(DecimalMeasure.valueOf("0 " + apparentPowerUOM));
		
		ApparentPowerStateValue apparentPowerStateL2 = new ApparentPowerStateValue();
		apparentPowerStateL2.setFeature("phaseID", "L2");
		apparentPowerStateL2.setValue(DecimalMeasure.valueOf("0 " + apparentPowerUOM));
		
		ApparentPowerStateValue apparentPowerStateL3 = new ApparentPowerStateValue();
		apparentPowerStateL3.setFeature("phaseID", "L3");
		apparentPowerStateL3.setValue(DecimalMeasure.valueOf("0 " + apparentPowerUOM));
		
		this.currentState.setState(ThreePhaseApparentPowerMeasurementState.class.getName(),
				new ThreePhaseApparentPowerMeasurementState(apparentPowerStateL1, apparentPowerStateL2,
						apparentPowerStateL3));
		// ----------------------------------------------------------------------
		
		// ------------------------ Single Phase Reactive Energy
		ReactiveEnergyStateValue reactiveEnergyStateValue = new ReactiveEnergyStateValue();
		reactiveEnergyStateValue.setValue(DecimalMeasure.valueOf("0 " + reactiveEnergyUOM));
		this.currentState.setState(SinglePhaseReactiveEnergyState.class.getName(), new SinglePhaseReactiveEnergyState(
				reactiveEnergyStateValue));
		// -----------------------------------------------------------------------
		
		// ------------------------ ThreePhaseLNVoltage
		VoltageStateValue voltageStateL1N = new VoltageStateValue();
		voltageStateL1N.setFeature("phaseID", "L1");
		voltageStateL1N.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		
		VoltageStateValue voltageStateL2N = new VoltageStateValue();
		voltageStateL2N.setFeature("phaseID", "L2");
		voltageStateL2N.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		
		VoltageStateValue voltageStateL3N = new VoltageStateValue();
		voltageStateL3N.setFeature("phaseID", "L3");
		voltageStateL3N.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		
		this.currentState.setState(ThreePhaseVoltageState.class.getName(), new ThreePhaseVoltageState(voltageStateL1N,
				voltageStateL2N, voltageStateL3N));
		// ----------------------------------------------------------
		
		// ------------------------ ThreePhaseLLVoltage
		VoltageStateValue voltageStateL12 = new VoltageStateValue();
		voltageStateL12.setFeature("phaseID", "L12");
		voltageStateL12.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		
		VoltageStateValue voltageStateL23 = new VoltageStateValue();
		voltageStateL23.setFeature("phaseID", "L23");
		voltageStateL23.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		
		VoltageStateValue voltageStateL31 = new VoltageStateValue();
		voltageStateL31.setFeature("phaseID", "L31");
		voltageStateL31.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		
		this.currentState.setState(ThreePhaseVoltageState.class.getName() + "LL", new ThreePhaseVoltageState(
				voltageStateL12, voltageStateL23, voltageStateL31));
		
		// read the initial state
		this.network.readAll(this.register2Notification.keySet());
		
	}
	
	private Measure<?, ?> getThreePhaseStateValue(String stateClass, String phaseID)
	{
		// the measure
		Measure<?, ?> value = null;
		
		// get the current state value
		StateValue currentStateValue[] = ((State) this.currentState.getState(stateClass)).getCurrentStateValue();
		
		// check which state value matches the given phase ID
		for (int i = 0; i < currentStateValue.length; i++)
		{
			// find the matching state value
			HashMap<String, Object> features = currentStateValue[i].getFeatures();
			
			// if matches, extract the power value and break the cycle
			if ((features.containsKey("phaseID")) && (((String) features.get("phaseID")).equalsIgnoreCase(phaseID)))
			{
				// extract the power value
				value = (Measure<?, ?>) currentStateValue[i].getValue();
				
				// break
				i = currentStateValue.length;
			}
		}
		return value;
	}
	
	private void updateThreePhaseStateValue(String stateClass, String phaseID, Measure<?, ?> value)
	{
		// update the state....
		StateValue currentStateValue[] = ((State) this.currentState.getState(stateClass)).getCurrentStateValue();
		for (int i = 0; i < currentStateValue.length; i++)
		{
			// find the matching state value
			HashMap<String, Object> features = currentStateValue[i].getFeatures();
			
			if ((features.containsKey("phaseID")) && (((String) features.get("phaseID")).equalsIgnoreCase(phaseID)))
			{
				// set the new value
				currentStateValue[i].setValue(DecimalMeasure.valueOf(value.toString()));
			}
		}
	}
	
	@Override
	protected void addToNetworkDriver(ModbusRegisterInfo register)
	{
		this.network.addDriver(register, this);
	}
	
}