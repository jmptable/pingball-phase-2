package boardfile;

import game.Absorber;
import game.Ball;
import game.Board;
import game.CircularBumper;
import game.Flipper;
import game.GamePiece;
import game.SquareBumper;
import game.TriangularBumper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import physics.Vect;
import server.PingballServer;

public class BoardFactory {

    /***
     * Parses the given input into a new Board.
     * @param input string representing a conjunctive boolean expression
     * @return new Board corresponding to the input
     */
    public static Board parse(String input) {
        // Create a stream of tokens using the lexer.
        CharStream stream = new ANTLRInputStream(input);
        BoardLexer lexer = new BoardLexer(stream);
        lexer.reportErrorsAsExceptions();
        TokenStream tokens = new CommonTokenStream(lexer);

        // Feed the tokens into the parser.
        BoardParser parser = new BoardParser(tokens);
        parser.reportErrorsAsExceptions();

        // Generate the parse tree using the starter rule.
        ParseTree tree = parser.board(); // "expression" is the starter rule

        // Finally, construct a Board value by walking over the parse tree.
        ParseTreeWalker walker = new ParseTreeWalker();
        BoardCreatorListener listener = new BoardCreatorListener();
        walker.walk(listener, tree);
        return listener.getBoard();
    }

    /***
     * Decodes the file at the given file path into text, using the given encoding.
     * @param path the file path of the file to decode
     * @param encoding the encodiding with which to decode the file
     * @return a text representation of the file
     * @throws IOException
     */
    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }    

    /***
     * BoardCreatorListener creates a new Board, updating the new Board
     * via a collection of methods converting parsing contexts into Board updates.
     */
    private static class BoardCreatorListener extends BoardBaseListener {
        private Set<Ball> balls = new HashSet<Ball>();
        private Map<GamePiece, Set<GamePiece>> actions = new HashMap<GamePiece, Set<GamePiece>>();
        private Map<String, GamePiece> gadgets = new HashMap<String, GamePiece>();
        private Board board;

        @Override public void enterSqbline(BoardParser.SqblineContext ctx) { 
            SquareBumper sqb = new SquareBumper(ctx.namefield().NAME().getText(), 
                    Integer.parseInt(ctx.xfield().INT().getText()), 
                    Integer.parseInt(ctx.yfield().INT().getText()));
            gadgets.put(ctx.namefield().NAME().getText(), sqb);
            actions.put(sqb, new HashSet<GamePiece>());
        }
        @Override public void enterCcbline(BoardParser.CcblineContext ctx) { 
            CircularBumper ccb = new CircularBumper(ctx.namefield().NAME().getText(), 
                    Integer.parseInt(ctx.xfield().INT().getText()), 
                    Integer.parseInt(ctx.yfield().INT().getText()));
            gadgets.put(ctx.namefield().NAME().getText(), ccb);
            actions.put(ccb, new HashSet<GamePiece>());
        }
        @Override public void enterTribline(BoardParser.TriblineContext ctx) { 
            TriangularBumper trib = new TriangularBumper(ctx.namefield().NAME().getText(), 
                    Integer.parseInt(ctx.xfield().INT().getText()), 
                    Integer.parseInt(ctx.yfield().INT().getText()),
                    Integer.parseInt(ctx.ortfield().INT().getText()));
            gadgets.put(ctx.namefield().NAME().getText(), trib);
            actions.put(trib, new HashSet<GamePiece>());
        }

        @Override public void enterAbsline(BoardParser.AbslineContext ctx) { 
            Absorber abs = new Absorber(ctx.namefield().NAME().getText(), 
                    Integer.parseInt(ctx.xfield().INT().getText()), 
                    Integer.parseInt(ctx.yfield().INT().getText()),
                    Integer.parseInt(ctx.widthfield().INT().getText()),
                    Integer.parseInt(ctx.heightfield().INT().getText()));
            gadgets.put(ctx.namefield().NAME().getText(), abs);
            actions.put(abs, new HashSet<GamePiece>());
        }

        @Override public void enterRightfline(BoardParser.RightflineContext ctx) { 
            Flipper flip = new Flipper("right", ctx.namefield().NAME().getText(), 
                    Integer.parseInt(ctx.xfield().INT().getText()), 
                    Integer.parseInt(ctx.yfield().INT().getText()),
                    Integer.parseInt(ctx.ortfield().INT().getText()));
            gadgets.put(ctx.namefield().NAME().getText(), flip);
            actions.put(flip, new HashSet<GamePiece>());
        }
        @Override public void enterLeftfline(BoardParser.LeftflineContext ctx) { 
            Flipper flip = new Flipper("left", ctx.namefield().NAME().getText(), 
                    Integer.parseInt(ctx.xfield().INT().getText()), 
                    Integer.parseInt(ctx.yfield().INT().getText()),
                    Integer.parseInt(ctx.ortfield().INT().getText()));
            gadgets.put(ctx.namefield().NAME().getText(), flip);
            actions.put(flip, new HashSet<GamePiece>());
        }

        @Override public void exitBoard(BoardParser.BoardContext ctx) { 
            double gravity = (ctx.topline().gravityfield()!=null) ? 
                Double.parseDouble(ctx.topline().gravityfield().FLOAT().getText()) : Board.DEFAULTGRAVITY;
            double mu1 = (ctx.topline().friction1field()!=null) ? 
                Double.parseDouble(ctx.topline().friction1field().FLOAT().getText()) : Board.DEFAULTMU1;
            double mu2 = (ctx.topline().friction2field()!=null) ? 
                Double.parseDouble(ctx.topline().friction2field().FLOAT().getText()) : Board.DEFAULTMU2;
            this.board = new Board(ctx.topline().namefield().NAME().getText(),gravity, mu1, mu2,actions, 1.0/(double)PingballServer.FRAMERATE, balls);
        }    

        @Override public void enterBallline(BoardParser.BalllineContext ctx) { 
            Ball ball = new Ball(ctx.namefield().NAME().getText(), 
                    new Vect(Double.parseDouble(ctx.xffield().FLOAT().getText()), 
                        Double.parseDouble(ctx.yffield().FLOAT().getText())), 
                    new Vect(Double.parseDouble(ctx.xvfield().FLOAT().getText()), 
                        Double.parseDouble(ctx.yvfield().FLOAT().getText()))); 
            balls.add(ball);    	
        }

        @Override public void enterFireline(BoardParser.FirelineContext ctx) { 
            actions.get(gadgets.get(ctx.triggerfield().NAME().getText())).add(
                    gadgets.get(ctx.actionfield().NAME().getText()));
        }

        public Board getBoard() {
            return this.board;
        }
    }



}
