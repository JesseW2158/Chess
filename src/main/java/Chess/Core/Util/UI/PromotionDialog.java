package Chess.Core.Util.UI;

import Chess.Core.Pieces.Pawn;
import Chess.Core.Pieces.Piece;
import Chess.Core.Pieces.Bishop;
import Chess.Core.Pieces.Knight;
import Chess.Core.Pieces.Queen;
import Chess.Core.Pieces.Rook;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

public class PromotionDialog extends Dialog<Piece> {
    private Piece selectedPiece;

    public PromotionDialog(Pawn pawn) {
        setTitle("Pawn Promotion " + pawn.getColor().getName());
        setResultConverter(f -> selectedPiece);
        HBox hbox = new HBox();
        hbox.getChildren().add(new PromotionCandidateLabel(new Queen(pawn.getColor(), null)));
        hbox.getChildren().add(new PromotionCandidateLabel(new Knight(pawn.getColor(), null)));
        hbox.getChildren().add(new PromotionCandidateLabel(new Rook(pawn.getColor(), null)));
        hbox.getChildren().add(new PromotionCandidateLabel(new Bishop(pawn.getColor(), null)));
        getDialogPane().setContent(hbox);
    }

    private class PromotionCandidateLabel extends Label {
        Piece piece;

        PromotionCandidateLabel(Piece piece) {
            setGraphic(piece.getImageView());
            this.piece = piece;
            setOnMouseReleased(this::onMouseReleased);
        }

        private void onMouseReleased(MouseEvent e) {
            selectedPiece = piece;
            getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
            close();
            e.consume();
        }
    }
}
