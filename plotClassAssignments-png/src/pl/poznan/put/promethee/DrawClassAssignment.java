package pl.poznan.put.promethee;

import pl.poznan.put.promethee.components.PaintComponent;
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
            output.setAssignmentsImage(PaintComponent.drawImage(inputs));
        } else {
            output.setAssignmentsImage(PaintComponent.drawImageVersion2(inputs));
        }

        return output;
    }

}
