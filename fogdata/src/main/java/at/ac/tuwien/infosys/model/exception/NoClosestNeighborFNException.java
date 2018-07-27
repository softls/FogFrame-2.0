package at.ac.tuwien.infosys.model.exception;

/**
 * Created by olena on 8/23/17.
 */
public class NoClosestNeighborFNException extends Exception {

    public NoClosestNeighborFNException() {
        super("The device tries to get the closest neighbor FN, there is noone to be assinged.");
    }
}
