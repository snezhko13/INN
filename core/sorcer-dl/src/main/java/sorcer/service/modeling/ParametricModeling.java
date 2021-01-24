/*
 * Distribution Statement
 * 
 * This computer software has been developed under sponsorship of the United States Air Force Research Lab. Any further
 * distribution or use by anyone or any data contained therein, unless otherwise specifically provided for,
 * is prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street Bldg 146, Room 218, WPAFB, OH  45433
 * 
 * Disclaimer
 * 
 * This material was prepared as an account of work sponsored by an agency of the United States Government. Neither
 * the United States Government nor the United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness
 * of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately
 * owned rights.
 */
package sorcer.service.modeling;

import sorcer.service.Configurable;
import sorcer.service.Context;
import sorcer.service.EvaluationException;
import sorcer.util.ModelTable;

import java.net.URL;
import java.rmi.RemoteException;

/**
 * @author Mike Sobolewski
 */
public interface ParametricModeling extends Configurable, Modeling {

	public Context evaluateResponseTable(Context<?> context)
			throws EvaluationException, RemoteException;

	public ModelTable getParametricTable() throws RemoteException;

	public void resetParametricTableURL(URL ptURL) throws RemoteException;
	
	public ModelTable getResponseTable() throws RemoteException;

	public void setResponseTableURL(URL rtURL) throws RemoteException;

}
