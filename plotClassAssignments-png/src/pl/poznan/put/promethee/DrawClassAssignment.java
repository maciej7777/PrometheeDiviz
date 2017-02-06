package pl.poznan.put.promethee;

import pl.poznan.put.promethee.xmcda.InputsHandler;
import pl.poznan.put.promethee.xmcda.OutputsHandler;

/**
 * Created by Maciej Uniejewski on 2016-12-26.
 */
public class DrawClassAssignment {

    private DrawClassAssignment() {

    }

    public static OutputsHandler.Output execute(InputsHandler.Inputs inputs) {
        OutputsHandler.Output output = new OutputsHandler.Output();

        if ("normal".equalsIgnoreCase(inputs.getVisualizationType().toString())) {
            output.setAssignmentsImage(pl.poznan.put.promethee.components.DrawClassAssignment.drawImage(inputs));
        } else {
            output.setAssignmentsImage(pl.poznan.put.promethee.components.DrawClassAssignment.drawImageVersion2(inputs));
        }

        return output;
    }

}
