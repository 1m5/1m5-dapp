package io.onemfive.dapp;

import io.onemfive.clearnet.server.EnvelopeJSONDataHandler;
import io.onemfive.core.Service;
import io.onemfive.core.admin.AdminService;
import io.onemfive.data.Envelope;
import io.onemfive.data.util.DLC;
import io.onemfive.data.util.JSONParser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handles JSON data requests.
 *
 * @author objectorange
 */
public class DataServiceHandler extends EnvelopeJSONDataHandler {

    private static Logger LOG = Logger.getLogger(DataServiceHandler.class.getName());

    public DataServiceHandler(){}

    /**
     * Pack Envelope into appropriate inbound request and route to bus
     * @param e
     */
    @Override
    protected void route(Envelope e) {
        String command = e.getCommandPath();
        Map<String,String> params = (Map<String,String>)DLC.getData(Map.class, e);
        switch(command) {
            case "list": {
                LOG.info("List Services request..");
                DLC.addData(List.class, new ArrayList<>(), e);
                DLC.addRoute(AdminService.class, AdminService.OPERATION_LIST_SERVICES, e);
                sensor.send(e);
                break;
            }
            case "register": {
                LOG.info("Register Service request...");
                String serviceConfig = params.get("s");
                List<Class> sList = new ArrayList<>();
                DLC.addEntity(sList, e);
                DLC.addData(Service.class, sList, e);
                DLC.addRoute(AdminService.class, AdminService.OPERATION_REGISTER_SERVICES, e);
                sensor.send(e);
                break;
            }
            default: {
                LOG.info("General request..");
                String service = params.get("s");
                String operation = params.get("op");
                DLC.addData(Map.class, params, e);
                try {
                    DLC.addRoute(Class.forName(service), operation, e);
                } catch (ClassNotFoundException ex) {
                    LOG.warning("Service not found by Classloader: "+service+"; dead lettering request.");
                }
            }
        }
    }

    @Override
    protected String unpackEnvelopeContent(Envelope e) {
        LOG.info("Unpacking Envelope...");
        Map<String,Object> m = new HashMap<>();
        String command = e.getCommandPath();
        m.put("command",command);
        switch(command) {
            case "list": {
                LOG.info("List Services response..");
                List lc = (List)DLC.getData(List.class,e);
                List<Map<String,Object>> results = new ArrayList<>();
                m.put("results",results);
                break;
            }
            case "Register": {
                LOG.info("Register response...");
                m.put("success","true");
                break;
            }
            default: {
                LOG.info("General response..");
                List<Map<String,Object>> results = (List<Map<String,Object>>)DLC.getContent(e);
                m.put("results", results);
            }

        }
        return JSONParser.toString(m);
    }

    private String getLocation(String locationParam) {
        if(locationParam!=null && !"".equals(locationParam)) {
            File f = new File(locationParam);
            if(!f.exists() && !f.mkdir()) {
                LOG.info("Unable to create user provided directory. Defaulting to: "+CoreDApp.oneMFiveAppContext.getDataDir().getAbsolutePath());
                return CoreDApp.oneMFiveAppContext.getDataDir().getAbsolutePath();
            } else {
                return locationParam;
            }
        } else {
            LOG.info("No location provided. Defaulting to: "+CoreDApp.oneMFiveAppContext.getDataDir().getAbsolutePath());
            return CoreDApp.oneMFiveAppContext.getDataDir().getAbsolutePath();
        }
    }
}
