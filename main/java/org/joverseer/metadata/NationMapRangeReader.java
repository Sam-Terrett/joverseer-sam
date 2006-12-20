package org.joverseer.metadata;

import org.joverseer.support.Container;
import org.joverseer.metadata.domain.ArtifactInfo;
import org.joverseer.metadata.domain.NationMapRange;
import org.springframework.richclient.application.Application;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;


public class NationMapRangeReader implements MetadataReader {
    String nationMapFilename = "maps.csv";

    public String getNationMapFilename(GameMetadata gm) {
        return "file:///" + gm.getBasePath() + "/" + gm.getGameType().toString() + "." + nationMapFilename;
    }
    public void load(GameMetadata gm) {
        Container mapRanges = new Container();

        try {
            Resource resource = Application.instance().getApplicationContext().getResource(getNationMapFilename(gm));

            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));

            String ln;
            while ((ln = reader.readLine()) != null) {
                NationMapRange nmr = new NationMapRange();
                String[] parts = ln.split(";");
                int nationNo = Integer.parseInt(parts[0]);
                int x1 = Integer.parseInt(parts[1]);
                int y1 = Integer.parseInt(parts[2]);
                int x2 = Integer.parseInt(parts[3]);
                int y2 = Integer.parseInt(parts[4]);
                mapRanges.addItem(nmr);
                nmr.setNationNo(nationNo);
                nmr.setTlX(x1);
                nmr.setTlY(y1);
                nmr.setBrX(x2);
                nmr.setBrY(y2);
           }
        }
        catch (IOException exc) {
            // todo see
            // do nothing
            int a = 1;
        }
        gm.setNationMapRanges(mapRanges);
    }
}