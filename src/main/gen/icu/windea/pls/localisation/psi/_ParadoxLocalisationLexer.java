// Generated by JFlex 1.9.1 http://jflex.de/  (tweaked for IntelliJ platform)
// source: ParadoxLocalisationLexer.flex

package icu.windea.pls.localisation.psi;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.*;
import static icu.windea.pls.core.StdlibExtensionsKt.*;
import static icu.windea.pls.localisation.psi.ParadoxLocalisationElementTypes.*;


public class _ParadoxLocalisationLexer implements FlexLexer {

  /** This character denotes the end of file */
  public static final int YYEOF = -1;

  /** initial size of the lookahead buffer */
  private static final int ZZ_BUFFERSIZE = 16384;

  /** lexical states */
  public static final int YYINITIAL = 0;
  public static final int IN_LOCALE_COLON = 2;
  public static final int IN_LOCALE_END = 4;
  public static final int IN_PROPERTY_COLON = 6;
  public static final int IN_PROPERTY_NUMBER = 8;
  public static final int IN_PROPERTY_VALUE = 10;
  public static final int IN_PROPERTY_END = 12;
  public static final int IN_RICH_TEXT = 14;
  public static final int IN_PROPERTY_REFERENCE = 16;
  public static final int IN_PROPERTY_REFERENCE_PARAMETER_TOKEN = 18;
  public static final int IN_SCRIPTED_VARIABLE_REFERENCE_NAME = 20;
  public static final int IN_ICON = 22;
  public static final int IN_ICON_ID_FINISHED = 24;
  public static final int IN_ICON_FRAME = 26;
  public static final int IN_ICON_FRAME_FINISHED = 28;
  public static final int IN_COMMAND = 30;
  public static final int IN_COMMAND_SCOPE_OR_FIELD = 32;
  public static final int IN_CONCEPT = 34;
  public static final int IN_CONCEPT_TEXT = 36;
  public static final int IN_COLOR_ID = 38;
  public static final int IN_COLORFUL_TEXT = 40;
  public static final int CHECK_PROPERTY_REFERENCE_START = 42;
  public static final int CHECK_ICON_START = 44;
  public static final int CHECK_COMMAND_START = 46;
  public static final int IN_CHECK_COLORFUL_TEXT_START = 48;
  public static final int CHECK_RIGHT_QUOTE = 50;

  /**
   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l
   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l
   *                  at the beginning of a line
   * l is of the form l = 2*k, k a non negative integer
   */
  private static final int ZZ_LEXSTATE[] = {
     0,  1,  2,  2,  3,  3,  4,  4,  5,  5,  6,  6,  7,  7,  8,  8, 
     9,  9, 10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 
    17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 23, 24, 24, 
    25, 25, 24, 24
  };

  /**
   * Top-level table for translating characters to character classes
   */
  private static final int [] ZZ_CMAP_TOP = zzUnpackcmap_top();

  private static final String ZZ_CMAP_TOP_PACKED_0 =
    "\1\0\5\u0100\1\u0200\1\u0300\1\u0100\5\u0400\1\u0500\1\u0600"+
    "\1\u0700\5\u0100\1\u0800\1\u0900\1\u0a00\1\u0b00\1\u0c00\1\u0d00"+
    "\1\u0e00\3\u0100\1\u0f00\17\u0100\1\u1000\165\u0100\1\u0600\1\u0100"+
    "\1\u1100\1\u1200\1\u1300\1\u1400\54\u0100\10\u1500\37\u0100\1\u0a00"+
    "\4\u0100\1\u1600\10\u0100\1\u1700\2\u0100\1\u1800\1\u1900\1\u1400"+
    "\1\u0100\1\u0500\1\u0100\1\u1a00\1\u1700\1\u0900\3\u0100\1\u1300"+
    "\1\u1b00\114\u0100\1\u1c00\1\u1300\153\u0100\1\u1d00\11\u0100\1\u1e00"+
    "\1\u1400\6\u0100\1\u1300\u0f16\u0100";

  private static int [] zzUnpackcmap_top() {
    int [] result = new int[4352];
    int offset = 0;
    offset = zzUnpackcmap_top(ZZ_CMAP_TOP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackcmap_top(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /**
   * Second-level tables for translating characters to character classes
   */
  private static final int [] ZZ_CMAP_BLOCKS = zzUnpackcmap_blocks();

  private static final String ZZ_CMAP_BLOCKS_PACKED_0 =
    "\11\0\1\1\1\2\2\3\1\4\22\0\1\1\1\5"+
    "\1\6\1\7\1\10\2\0\1\11\4\0\1\12\1\13"+
    "\1\14\1\15\1\16\11\17\1\20\5\0\1\21\32\22"+
    "\1\23\1\24\1\25\1\0\1\26\1\0\32\27\1\0"+
    "\1\30\10\0\1\3\32\0\1\1\2\0\1\31\3\0"+
    "\1\32\u01b8\0\12\33\206\0\12\33\306\0\12\33\234\0"+
    "\12\33\166\0\12\33\140\0\12\33\166\0\12\33\106\0"+
    "\12\33\u0116\0\12\33\106\0\12\33\346\0\1\1\u015f\0"+
    "\12\33\46\0\12\33\u012c\0\12\33\200\0\12\33\246\0"+
    "\12\33\6\0\12\33\266\0\12\33\126\0\12\33\206\0"+
    "\12\33\6\0\12\33\246\0\13\1\35\0\2\3\5\0"+
    "\1\1\57\0\1\1\240\0\1\1\u01cf\0\12\33\46\0"+
    "\12\33\306\0\12\33\26\0\12\33\126\0\12\33\u0196\0"+
    "\12\33\6\0\u0100\34\240\0\12\33\206\0\12\33\u012c\0"+
    "\12\33\200\0\12\33\74\0\12\33\220\0\12\33\166\0"+
    "\12\33\146\0\12\33\206\0\12\33\106\0\12\33\266\0"+
    "\12\33\u0164\0\62\33\100\0\12\33\266\0";

  private static int [] zzUnpackcmap_blocks() {
    int [] result = new int[7936];
    int offset = 0;
    offset = zzUnpackcmap_blocks(ZZ_CMAP_BLOCKS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackcmap_blocks(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /**
   * Translates DFA states to action switch labels.
   */
  private static final int [] ZZ_ACTION = zzUnpackAction();

  private static final String ZZ_ACTION_PACKED_0 =
    "\32\0\1\1\1\2\1\3\1\4\2\5\1\6\1\7"+
    "\1\10\1\11\1\12\1\13\2\14\1\15\1\16\1\1"+
    "\1\17\1\20\1\21\1\22\1\23\1\24\1\25\1\26"+
    "\1\27\1\30\1\22\1\31\1\32\1\33\1\34\1\35"+
    "\1\36\1\37\1\40\1\41\1\42\1\1\1\2\1\13"+
    "\1\43\1\44\1\45\1\21\1\46\1\47\1\50\1\51"+
    "\1\52\1\53\1\54\1\53\1\55\1\21\1\1\1\56"+
    "\1\57\1\60\4\0\1\61\1\0\1\62\1\63\1\61"+
    "\1\64\2\56\1\57\1\60\2\65\1\66";

  private static int [] zzUnpackAction() {
    int [] result = new int[102];
    int offset = 0;
    offset = zzUnpackAction(ZZ_ACTION_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAction(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /**
   * Translates a state to a row index in the transition table
   */
  private static final int [] ZZ_ROWMAP = zzUnpackRowMap();

  private static final String ZZ_ROWMAP_PACKED_0 =
    "\0\0\0\35\0\72\0\127\0\164\0\221\0\256\0\313"+
    "\0\350\0\u0105\0\u0122\0\u013f\0\u015c\0\u0179\0\u0196\0\u01b3"+
    "\0\u01d0\0\u01ed\0\u020a\0\u0227\0\u0244\0\u0261\0\u027e\0\u029b"+
    "\0\u02b8\0\u02d5\0\u02f2\0\u030f\0\u032c\0\u0349\0\u0366\0\u0383"+
    "\0\u02f2\0\u02f2\0\u03a0\0\u02f2\0\u03bd\0\u02f2\0\u03da\0\u03f7"+
    "\0\u02f2\0\u0414\0\u0431\0\u02f2\0\u02f2\0\u044e\0\u046b\0\u02f2"+
    "\0\u0488\0\u02f2\0\u02f2\0\u02f2\0\u04a5\0\u04c2\0\u04df\0\u02f2"+
    "\0\u02f2\0\u04fc\0\u02f2\0\u02f2\0\u02f2\0\u02f2\0\u0519\0\u02f2"+
    "\0\u0536\0\u0553\0\u0536\0\u0536\0\u02f2\0\u02f2\0\u0570\0\u02f2"+
    "\0\u02f2\0\u058d\0\u02f2\0\u0414\0\u02f2\0\u05aa\0\u032c\0\u02f2"+
    "\0\u05c7\0\u032c\0\u05e4\0\u0601\0\u061e\0\u032c\0\u063b\0\u0414"+
    "\0\u0431\0\u02f2\0\u0536\0\u02f2\0\u02f2\0\u0536\0\u02f2\0\u02f2"+
    "\0\u0658\0\u02f2\0\u02f2\0\u063b\0\u0675\0\u063b";

  private static int [] zzUnpackRowMap() {
    int [] result = new int[102];
    int offset = 0;
    offset = zzUnpackRowMap(ZZ_ROWMAP_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackRowMap(String packed, int offset, int [] result) {
    int i = 0;  /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length() - 1;
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    }
    return j;
  }

  /**
   * The transition table of the DFA
   */
  private static final int [] ZZ_TRANS = zzUnpacktrans();

  private static final String ZZ_TRANS_PACKED_0 =
    "\1\33\1\34\1\35\1\34\1\35\2\33\1\36\1\33"+
    "\1\37\1\33\2\37\1\33\2\37\2\33\1\37\3\33"+
    "\2\37\6\33\1\34\1\35\1\34\1\35\2\33\1\36"+
    "\1\33\1\37\1\33\2\37\1\33\2\37\2\33\1\37"+
    "\3\33\2\40\6\33\1\34\1\35\1\34\1\35\13\33"+
    "\1\41\15\33\1\34\1\35\1\34\1\35\2\33\1\36"+
    "\26\33\1\34\1\35\1\34\1\35\13\33\1\42\15\33"+
    "\1\43\1\35\1\43\1\35\1\33\1\44\7\33\2\45"+
    "\13\33\1\45\2\33\1\34\1\35\1\34\1\35\1\33"+
    "\1\44\27\33\1\34\1\35\1\34\1\35\1\33\1\46"+
    "\1\36\25\33\1\47\1\50\1\35\1\50\1\35\1\47"+
    "\1\46\1\47\1\51\12\47\1\52\1\53\1\54\3\47"+
    "\1\55\1\56\2\47\1\33\1\57\1\35\1\57\1\35"+
    "\1\33\1\46\1\33\1\60\1\61\1\33\2\61\1\33"+
    "\2\61\1\33\1\62\1\61\1\63\2\33\2\61\1\64"+
    "\1\33\1\56\2\33\1\65\1\66\1\35\1\66\1\35"+
    "\1\65\1\46\1\65\1\60\12\65\2\33\4\65\1\33"+
    "\1\56\2\65\1\33\1\57\1\35\1\57\1\35\1\33"+
    "\1\46\1\33\1\60\11\33\1\67\1\70\2\33\2\67"+
    "\1\64\1\33\1\56\3\33\1\57\1\35\1\57\1\35"+
    "\1\33\1\46\1\33\1\71\2\33\1\72\1\33\3\72"+
    "\2\33\1\72\1\73\1\72\1\33\2\72\1\74\1\75"+
    "\1\56\3\33\1\57\1\35\1\57\1\35\1\33\1\46"+
    "\21\33\1\74\1\75\1\56\3\33\1\57\1\35\1\57"+
    "\1\35\1\33\1\46\1\33\1\76\6\33\1\77\11\33"+
    "\1\75\1\56\3\33\1\57\1\35\1\57\1\35\1\33"+
    "\1\46\22\33\1\75\1\56\2\33\1\100\1\34\1\35"+
    "\1\34\1\35\27\100\1\33\1\101\1\102\1\35\1\102"+
    "\1\35\1\101\1\103\1\101\1\104\3\101\1\105\6\101"+
    "\1\33\1\101\1\106\4\101\1\107\2\101\1\33\1\34"+
    "\1\35\1\34\1\35\1\33\1\46\2\33\1\110\1\111"+
    "\3\33\2\112\2\33\1\112\2\33\1\113\2\112\2\33"+
    "\1\56\2\33\1\47\1\50\1\35\1\50\1\35\1\47"+
    "\1\46\1\47\1\51\12\47\1\114\1\53\1\113\3\47"+
    "\1\55\1\56\2\47\1\115\1\116\1\117\1\116\1\117"+
    "\1\115\1\46\7\115\2\120\2\115\1\120\4\115\1\120"+
    "\2\115\1\121\2\115\1\47\1\50\1\35\1\50\1\35"+
    "\1\47\1\46\1\47\1\51\12\47\1\114\1\53\1\54"+
    "\3\47\1\55\1\56\2\47\1\33\1\122\3\35\3\33"+
    "\1\123\25\33\1\122\3\35\24\33\1\124\4\33\1\122"+
    "\3\35\31\33\1\122\3\35\25\33\1\125\2\33\36\0"+
    "\1\34\1\35\1\34\1\35\31\0\1\126\3\35\30\0"+
    "\2\36\1\0\1\36\1\0\30\36\11\0\1\37\1\0"+
    "\2\37\1\0\2\37\2\0\1\37\3\0\2\37\16\0"+
    "\1\37\1\0\2\37\1\0\2\37\1\127\1\0\1\37"+
    "\3\0\2\40\6\0\1\43\1\35\1\43\1\35\46\0"+
    "\2\45\13\0\1\45\1\0\2\47\1\0\1\47\1\0"+
    "\1\47\1\0\1\47\1\0\12\47\1\130\1\131\1\0"+
    "\3\47\2\0\3\47\1\50\1\35\1\50\1\35\1\47"+
    "\1\0\1\47\1\0\12\47\1\130\1\131\1\0\3\47"+
    "\2\0\2\47\23\0\1\47\11\0\2\47\3\0\27\47"+
    "\6\0\1\132\30\0\1\57\1\35\1\57\1\35\41\0"+
    "\1\61\1\0\2\61\1\0\2\61\2\0\1\61\3\0"+
    "\2\61\5\0\2\65\1\0\1\65\1\0\1\65\1\0"+
    "\1\65\1\0\12\65\2\0\4\65\2\0\3\65\1\66"+
    "\1\35\1\66\1\35\1\65\1\0\1\65\1\0\12\65"+
    "\2\0\4\65\2\0\2\65\16\0\2\67\2\0\1\67"+
    "\3\0\2\67\20\0\1\72\1\0\3\72\2\0\1\72"+
    "\1\0\1\72\1\0\2\72\23\0\2\77\15\0\2\133"+
    "\1\0\1\133\1\0\7\133\1\134\6\133\1\0\1\133"+
    "\1\135\10\133\1\102\1\35\1\102\1\35\7\133\1\134"+
    "\6\133\1\0\1\133\1\135\11\133\1\0\1\133\1\0"+
    "\1\136\6\133\1\134\6\133\1\0\1\133\1\135\7\133"+
    "\16\0\2\112\2\0\1\112\3\0\2\112\6\0\1\116"+
    "\1\35\1\116\1\35\35\0\1\137\40\0\1\140\1\0"+
    "\2\140\1\0\2\140\1\0\2\140\1\141\2\0\2\140"+
    "\5\0\2\142\3\0\27\142\1\0\2\143\3\0\27\143"+
    "\2\0\1\127\2\144\1\145\30\0\2\141\1\0\1\141"+
    "\1\0\20\141\1\140\7\141\1\0\1\127\1\146\1\144"+
    "\1\145\30\0";

  private static int [] zzUnpacktrans() {
    int [] result = new int[1682];
    int offset = 0;
    offset = zzUnpacktrans(ZZ_TRANS_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpacktrans(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      value--;
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }


  /* error codes */
  private static final int ZZ_UNKNOWN_ERROR = 0;
  private static final int ZZ_NO_MATCH = 1;
  private static final int ZZ_PUSHBACK_2BIG = 2;

  /* error messages for the codes above */
  private static final String[] ZZ_ERROR_MSG = {
    "Unknown internal scanner error",
    "Error: could not match input",
    "Error: pushback value was too large"
  };

  /**
   * ZZ_ATTRIBUTE[aState] contains the attributes of state {@code aState}
   */
  private static final int [] ZZ_ATTRIBUTE = zzUnpackAttribute();

  private static final String ZZ_ATTRIBUTE_PACKED_0 =
    "\32\0\1\11\5\1\2\11\1\1\1\11\1\1\1\11"+
    "\2\1\1\11\2\1\2\11\2\1\1\11\1\1\3\11"+
    "\3\1\2\11\1\1\4\11\1\1\1\11\4\1\2\11"+
    "\1\1\2\11\1\1\1\11\1\1\1\11\2\1\1\11"+
    "\5\1\4\0\1\11\1\0\2\11\1\1\2\11\1\1"+
    "\2\11\3\1";

  private static int [] zzUnpackAttribute() {
    int [] result = new int[102];
    int offset = 0;
    offset = zzUnpackAttribute(ZZ_ATTRIBUTE_PACKED_0, offset, result);
    return result;
  }

  private static int zzUnpackAttribute(String packed, int offset, int [] result) {
    int i = 0;       /* index in packed string  */
    int j = offset;  /* index in unpacked array */
    int l = packed.length();
    while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      do result[j++] = value; while (--count > 0);
    }
    return j;
  }

  /** the input device */
  private java.io.Reader zzReader;

  /** the current state of the DFA */
  private int zzState;

  /** the current lexical state */
  private int zzLexicalState = YYINITIAL;

  /** this buffer contains the current text to be matched and is
      the source of the yytext() string */
  private CharSequence zzBuffer = "";

  /** the textposition at the last accepting state */
  private int zzMarkedPos;

  /** the current text position in the buffer */
  private int zzCurrentPos;

  /** startRead marks the beginning of the yytext() string in the buffer */
  private int zzStartRead;

  /** endRead marks the last character in the buffer, that has been read
      from input */
  private int zzEndRead;

  /** zzAtEOF == true <=> the scanner is at the EOF */
  private boolean zzAtEOF;

  /** Number of newlines encountered up to the start of the matched text. */
  @SuppressWarnings("unused")
  private int yyline;

  /** Number of characters from the last newline up to the start of the matched text. */
  @SuppressWarnings("unused")
  protected int yycolumn;

  /** Number of characters up to the start of the matched text. */
  @SuppressWarnings("unused")
  private long yychar;

  /** Whether the scanner is currently at the beginning of a line. */
  private boolean zzAtBOL = true;

  /** Whether the user-EOF-code has already been executed. */
  @SuppressWarnings("unused")
  private boolean zzEOFDone;

  /* user code: */
    private int depth = 0;
    private boolean inConceptText = false;
    private CommandLocation commandLocation = CommandLocation.NORMAL;
    private ReferenceLocation referenceLocation = ReferenceLocation.NORMAL;
    
    public _ParadoxLocalisationLexer() {
        this((java.io.Reader)null);
    }
    
    private void increaseDepth(){
        depth++;
    }
    
    private void decreaseDepth(){
        if(depth > 0) depth--;
    }
    
    private int nextStateForText(){
      return depth <= 0 ? IN_RICH_TEXT : IN_COLORFUL_TEXT;
    }
    
    private enum CommandLocation {
        NORMAL, REFERENCE, ICON;
    }
    
    private int nextStateForCommand(){
        return switch(commandLocation) {
            case NORMAL -> nextStateForText();
            case REFERENCE -> IN_PROPERTY_REFERENCE;
            case ICON -> IN_ICON;
        };
    }
    
    private enum ReferenceLocation {
        NORMAL, ICON, ICON_FRAME, COMMAND;
    }
    
    private int nextStateForPropertyReference(){
        return switch(referenceLocation) {
            case NORMAL -> nextStateForText();
            case ICON -> IN_ICON_ID_FINISHED;
            case ICON_FRAME -> IN_ICON_FRAME_FINISHED;
            case COMMAND -> IN_COMMAND_SCOPE_OR_FIELD;
        };
    }
    
    private boolean isReferenceStart(){
        if(yylength() <= 1) return false;
        return true;
    }
    
    private boolean isIconStart(){
        if(yylength() <= 1) return false;
        char c = yycharat(1);
        return isExactLetter(c) || isExactDigit(c) || c == '_' || c == '$';
    }
    
    private boolean isCommandStart(){
        if(yylength() <= 1) return false;
        return yycharat(yylength()-1) == ']';
    }
    
    private boolean isColorfulTextStart(){
        if(yylength() <= 1) return false;
        return isExactLetter(yycharat(1));
    }
    
    private IElementType checkRightQuote() {
        //NOTE double quote should be threat as a string if it's not the last one of current line
        try {
            int i = zzCurrentPos + 1;
            int length = zzBuffer.length();
            while(i < length) {
                char c = zzBuffer.charAt(i);
                if(Character.isWhitespace(c)) break;
                if(c == '"') return STRING_TOKEN;
                i++;
            }
        } catch(Exception e) {
            //ignored
        }
        
        yybegin(IN_PROPERTY_END);
	    return RIGHT_QUOTE;
    }


  /**
   * Creates a new scanner
   *
   * @param   in  the java.io.Reader to read input from.
   */
  public _ParadoxLocalisationLexer(java.io.Reader in) {
    this.zzReader = in;
  }


  /** Returns the maximum size of the scanner buffer, which limits the size of tokens. */
  private int zzMaxBufferLen() {
    return Integer.MAX_VALUE;
  }

  /**  Whether the scanner buffer can grow to accommodate a larger token. */
  private boolean zzCanGrow() {
    return true;
  }

  /**
   * Translates raw input code points to DFA table row
   */
  private static int zzCMap(int input) {
    int offset = input & 255;
    return offset == input ? ZZ_CMAP_BLOCKS[offset] : ZZ_CMAP_BLOCKS[ZZ_CMAP_TOP[input >> 8] | offset];
  }

  public final int getTokenStart() {
    return zzStartRead;
  }

  public final int getTokenEnd() {
    return getTokenStart() + yylength();
  }

  public void reset(CharSequence buffer, int start, int end, int initialState) {
    zzBuffer = buffer;
    zzCurrentPos = zzMarkedPos = zzStartRead = start;
    zzAtEOF  = false;
    zzAtBOL = true;
    zzEndRead = end;
    yybegin(initialState);
  }

  /**
   * Refills the input buffer.
   *
   * @return      {@code false}, iff there was new input.
   *
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  private boolean zzRefill() throws java.io.IOException {
    return true;
  }


  /**
   * Returns the current lexical state.
   */
  public final int yystate() {
    return zzLexicalState;
  }


  /**
   * Enters a new lexical state
   *
   * @param newState the new lexical state
   */
  public final void yybegin(int newState) {
    zzLexicalState = newState;
  }


  /**
   * Returns the text matched by the current regular expression.
   */
  public final CharSequence yytext() {
    return zzBuffer.subSequence(zzStartRead, zzMarkedPos);
  }


  /**
   * Returns the character at position {@code pos} from the
   * matched text.
   *
   * It is equivalent to yytext().charAt(pos), but faster
   *
   * @param pos the position of the character to fetch.
   *            A value from 0 to yylength()-1.
   *
   * @return the character at position pos
   */
  public final char yycharat(int pos) {
    return zzBuffer.charAt(zzStartRead+pos);
  }


  /**
   * Returns the length of the matched text region.
   */
  public final int yylength() {
    return zzMarkedPos-zzStartRead;
  }


  /**
   * Reports an error that occurred while scanning.
   *
   * In a wellformed scanner (no or only correct usage of
   * yypushback(int) and a match-all fallback rule) this method
   * will only be called with things that "Can't Possibly Happen".
   * If this method is called, something is seriously wrong
   * (e.g. a JFlex bug producing a faulty scanner etc.).
   *
   * Usual syntax/scanner level error handling should be done
   * in error fallback rules.
   *
   * @param   errorCode  the code of the errormessage to display
   */
  private void zzScanError(int errorCode) {
    String message;
    try {
      message = ZZ_ERROR_MSG[errorCode];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      message = ZZ_ERROR_MSG[ZZ_UNKNOWN_ERROR];
    }

    throw new Error(message);
  }


  /**
   * Pushes the specified amount of characters back into the input stream.
   *
   * They will be read again by then next call of the scanning method
   *
   * @param number  the number of characters to be read again.
   *                This number must not be greater than yylength()!
   */
  public void yypushback(int number)  {
    if ( number > yylength() )
      zzScanError(ZZ_PUSHBACK_2BIG);

    zzMarkedPos -= number;
  }


  /**
   * Resumes scanning until the next regular expression is matched,
   * the end of input is encountered or an I/O-Error occurs.
   *
   * @return      the next token
   * @exception   java.io.IOException  if any I/O-Error occurs
   */
  public IElementType advance() throws java.io.IOException
  {
    int zzInput;
    int zzAction;

    // cached fields:
    int zzCurrentPosL;
    int zzMarkedPosL;
    int zzEndReadL = zzEndRead;
    CharSequence zzBufferL = zzBuffer;

    int [] zzTransL = ZZ_TRANS;
    int [] zzRowMapL = ZZ_ROWMAP;
    int [] zzAttrL = ZZ_ATTRIBUTE;

    while (true) {
      zzMarkedPosL = zzMarkedPos;

      if (zzMarkedPosL > zzStartRead) {
        switch (zzBufferL.charAt(zzMarkedPosL-1)) {
        case '\n':
        case '\u000B':  // fall through
        case '\u000C':  // fall through
        case '\u0085':  // fall through
        case '\u2028':  // fall through
        case '\u2029':  // fall through
          zzAtBOL = true;
          break;
        case '\r': 
          if (zzMarkedPosL < zzEndReadL)
            zzAtBOL = zzBufferL.charAt(zzMarkedPosL) != '\n';
          else if (zzAtEOF)
            zzAtBOL = false;
          else {
            boolean eof = zzRefill();
            zzMarkedPosL = zzMarkedPos;
            zzEndReadL = zzEndRead;
            zzBufferL = zzBuffer;
            if (eof) 
              zzAtBOL = false;
            else 
              zzAtBOL = zzBufferL.charAt(zzMarkedPosL) != '\n';
          }
          break;
        default:
          zzAtBOL = false;
        }
      }
      zzAction = -1;

      zzCurrentPosL = zzCurrentPos = zzStartRead = zzMarkedPosL;

      if (zzAtBOL)
        zzState = ZZ_LEXSTATE[zzLexicalState+1];
      else
        zzState = ZZ_LEXSTATE[zzLexicalState];

      // set up zzAction for empty match case:
      int zzAttributes = zzAttrL[zzState];
      if ( (zzAttributes & 1) == 1 ) {
        zzAction = zzState;
      }


      zzForAction: {
        while (true) {

          if (zzCurrentPosL < zzEndReadL) {
            zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL);
            zzCurrentPosL += Character.charCount(zzInput);
          }
          else if (zzAtEOF) {
            zzInput = YYEOF;
            break zzForAction;
          }
          else {
            // store back cached positions
            zzCurrentPos  = zzCurrentPosL;
            zzMarkedPos   = zzMarkedPosL;
            boolean eof = zzRefill();
            // get translated positions and possibly new buffer
            zzCurrentPosL  = zzCurrentPos;
            zzMarkedPosL   = zzMarkedPos;
            zzBufferL      = zzBuffer;
            zzEndReadL     = zzEndRead;
            if (eof) {
              zzInput = YYEOF;
              break zzForAction;
            }
            else {
              zzInput = Character.codePointAt(zzBufferL, zzCurrentPosL);
              zzCurrentPosL += Character.charCount(zzInput);
            }
          }
          int zzNext = zzTransL[ zzRowMapL[zzState] + zzCMap(zzInput) ];
          if (zzNext == -1) break zzForAction;
          zzState = zzNext;

          zzAttributes = zzAttrL[zzState];
          if ( (zzAttributes & 1) == 1 ) {
            zzAction = zzState;
            zzMarkedPosL = zzCurrentPosL;
            if ( (zzAttributes & 8) == 8 ) break zzForAction;
          }

        }
      }

      // store back cached position
      zzMarkedPos = zzMarkedPosL;

      if (zzInput == YYEOF && zzStartRead == zzCurrentPos) {
        zzAtEOF = true;
        return null;
      }
      else {
        switch (zzAction < 0 ? zzAction : ZZ_ACTION[zzAction]) {
          case 1:
            { return BAD_CHARACTER;
            }
          // fall through
          case 55: break;
          case 2:
            { return WHITE_SPACE;
            }
          // fall through
          case 56: break;
          case 3:
            { depth=0; inConceptText=false; yybegin(YYINITIAL); return WHITE_SPACE;
            }
          // fall through
          case 57: break;
          case 4:
            { return COMMENT;
            }
          // fall through
          case 58: break;
          case 5:
            { yybegin(IN_PROPERTY_COLON);
        return PROPERTY_KEY_TOKEN;
            }
          // fall through
          case 59: break;
          case 6:
            { yybegin(IN_LOCALE_END); return COLON;
            }
          // fall through
          case 60: break;
          case 7:
            { yybegin(IN_PROPERTY_NUMBER); return COLON;
            }
          // fall through
          case 61: break;
          case 8:
            { yybegin(IN_PROPERTY_VALUE); return WHITE_SPACE;
            }
          // fall through
          case 62: break;
          case 9:
            { yybegin(IN_RICH_TEXT); return LEFT_QUOTE;
            }
          // fall through
          case 63: break;
          case 10:
            { yybegin(IN_PROPERTY_VALUE); return PROPERTY_NUMBER;
            }
          // fall through
          case 64: break;
          case 11:
            { return checkRightQuote();
            }
          // fall through
          case 65: break;
          case 12:
            { return STRING_TOKEN;
            }
          // fall through
          case 66: break;
          case 13:
            { referenceLocation=ReferenceLocation.NORMAL; yypushback(yylength()); yybegin(CHECK_PROPERTY_REFERENCE_START);
            }
          // fall through
          case 67: break;
          case 14:
            { increaseDepth(); commandLocation=CommandLocation.NORMAL; yybegin(IN_COMMAND); return COMMAND_START;
            }
          // fall through
          case 68: break;
          case 15:
            { if(inConceptText) {
            inConceptText = false;
            decreaseDepth();
            yybegin(nextStateForCommand());
            return COMMAND_END;
        }
        return STRING_TOKEN;
            }
          // fall through
          case 69: break;
          case 16:
            { yypushback(yylength()); yybegin(CHECK_ICON_START);
            }
          // fall through
          case 70: break;
          case 17:
            { yypushback(yylength()); yybegin(IN_CHECK_COLORFUL_TEXT_START);
            }
          // fall through
          case 71: break;
          case 18:
            { yybegin(nextStateForText()); return WHITE_SPACE;
            }
          // fall through
          case 72: break;
          case 19:
            { yybegin(nextStateForPropertyReference()); return PROPERTY_REFERENCE_END;
            }
          // fall through
          case 73: break;
          case 20:
            { return PROPERTY_REFERENCE_TOKEN;
            }
          // fall through
          case 74: break;
          case 21:
            { yybegin(IN_SCRIPTED_VARIABLE_REFERENCE_NAME); return AT;
            }
          // fall through
          case 75: break;
          case 22:
            { increaseDepth();commandLocation=CommandLocation.REFERENCE; yybegin(IN_COMMAND); return COMMAND_START;
            }
          // fall through
          case 76: break;
          case 23:
            { yybegin(IN_PROPERTY_REFERENCE_PARAMETER_TOKEN); return PIPE;
            }
          // fall through
          case 77: break;
          case 24:
            { return PROPERTY_REFERENCE_PARAMETER_TOKEN;
            }
          // fall through
          case 78: break;
          case 25:
            { return SCRIPTED_VARIABLE_REFERENCE_TOKEN;
            }
          // fall through
          case 79: break;
          case 26:
            { increaseDepth();commandLocation=CommandLocation.REFERENCE; yybegin(IN_COMMAND);return COMMAND_START;
            }
          // fall through
          case 80: break;
          case 27:
            { referenceLocation=ReferenceLocation.ICON; yypushback(yylength()); yybegin(CHECK_PROPERTY_REFERENCE_START);
            }
          // fall through
          case 81: break;
          case 28:
            { yybegin(IN_ICON_ID_FINISHED); return ICON_TOKEN;
            }
          // fall through
          case 82: break;
          case 29:
            { increaseDepth(); commandLocation=CommandLocation.ICON; yybegin(IN_COMMAND); return COMMAND_START;
            }
          // fall through
          case 83: break;
          case 30:
            { yybegin(IN_ICON_FRAME); return PIPE;
            }
          // fall through
          case 84: break;
          case 31:
            { yybegin(nextStateForText()); return ICON_END;
            }
          // fall through
          case 85: break;
          case 32:
            { referenceLocation=ReferenceLocation.ICON_FRAME; yypushback(yylength()); yybegin(CHECK_PROPERTY_REFERENCE_START);
            }
          // fall through
          case 86: break;
          case 33:
            { yybegin(IN_ICON_FRAME_FINISHED); return ICON_FRAME;
            }
          // fall through
          case 87: break;
          case 34:
            { if(yycharat(0) == '\'') {
            yybegin(IN_CONCEPT);
            return LEFT_SINGLE_QUOTE;
        } else {
            yypushback(1);
            yybegin(IN_COMMAND_SCOPE_OR_FIELD);
        }
            }
          // fall through
          case 88: break;
          case 35:
            { referenceLocation=ReferenceLocation.COMMAND; yypushback(yylength()); yybegin(CHECK_PROPERTY_REFERENCE_START);
            }
          // fall through
          case 89: break;
          case 36:
            { yybegin(IN_COMMAND_SCOPE_OR_FIELD); return DOT;
            }
          // fall through
          case 90: break;
          case 37:
            { decreaseDepth(); yybegin(nextStateForCommand()); return COMMAND_END;
            }
          // fall through
          case 91: break;
          case 38:
            { return RIGHT_SINGLE_QUOTE;
            }
          // fall through
          case 92: break;
          case 39:
            { inConceptText=true; yybegin(IN_CONCEPT_TEXT); return COMMA;
            }
          // fall through
          case 93: break;
          case 40:
            { return CONCEPT_NAME_TOKEN;
            }
          // fall through
          case 94: break;
          case 41:
            { decreaseDepth();yybegin(nextStateForCommand()); return COMMAND_END;
            }
          // fall through
          case 95: break;
          case 42:
            { increaseDepth();commandLocation=CommandLocation.NORMAL; yybegin(IN_COMMAND);return COMMAND_START;
            }
          // fall through
          case 96: break;
          case 43:
            { yypushback(yylength()); yybegin(IN_COLORFUL_TEXT);
            }
          // fall through
          case 97: break;
          case 44:
            { yybegin(IN_COLORFUL_TEXT); return WHITE_SPACE;
            }
          // fall through
          case 98: break;
          case 45:
            { yybegin(IN_COLORFUL_TEXT); return COLOR_TOKEN;
            }
          // fall through
          case 99: break;
          case 46:
            { //特殊处理
            //如果匹配到的字符串长度大于1，且"$"后面的字符可以被识别为PROPERTY_REFERENCE_TOKEN或者command，或者是@，则认为代表属性引用的开始
            boolean isReferenceStart = isReferenceStart();
            yypushback(yylength()-1);
            if(isReferenceStart){
                yybegin(IN_PROPERTY_REFERENCE);
                return PROPERTY_REFERENCE_START;
            } else {
                yybegin(nextStateForText());
                return STRING_TOKEN;
            }
            }
          // fall through
          case 100: break;
          case 47:
            { //特殊处理
            //如果匹配到的字符串的第2个字符存在且为字母、数字或下划线或者$，则认为代表图标的开始
            //否则认为是常规字符串
            boolean isIconStart = isIconStart();
            yypushback(yylength()-1);
            if(isIconStart){
                yybegin(IN_ICON);
                return ICON_START;
            }else{
                yybegin(nextStateForText());
                return STRING_TOKEN;
            }
            }
          // fall through
          case 101: break;
          case 48:
            { //特殊处理
            //如果匹配到的字符串的第2个字符存在且为字母，则认为代表彩色文本的开始
            //否则认为是常规字符串
            boolean isColorfulTextStart = isColorfulTextStart();
            yypushback(yylength()-1);
            if(isColorfulTextStart) {
                yybegin(IN_COLOR_ID);
                increaseDepth();
                return COLORFUL_TEXT_START;
            } else {
                yybegin(nextStateForText());
                return STRING_TOKEN;
            }
            }
          // fall through
          case 102: break;
          case 49:
            { decreaseDepth(); yybegin(nextStateForText()); return COLORFUL_TEXT_END;
            }
          // fall through
          case 103: break;
          case 50:
            { yypushback(1); return COMMAND_SCOPE_TOKEN;
            }
          // fall through
          case 104: break;
          case 51:
            { yypushback(1); return COMMAND_FIELD_TOKEN;
            }
          // fall through
          case 105: break;
          case 52:
            { decreaseDepth(); decreaseDepth(); yybegin(nextStateForText()); return COLORFUL_TEXT_END;
            }
          // fall through
          case 106: break;
          case 53:
            // lookahead expression with fixed lookahead length
            zzMarkedPos = Character.offsetByCodePoints
                (zzBufferL, zzMarkedPos, -1);
            { //本地化文件中可以没有，或者有多个locale - 主要是为了兼容localisation/languages.yml
        //locale之前必须没有任何缩进
        //locale之后的冒号和换行符之间应当没有任何字符或者只有空白字符
        //采用最简单的实现方式，尽管JFlex手册中说 "^" "$" 性能不佳
        int n = 1;
        int l = yylength();
        while(Character.isWhitespace(yycharat(l - n))) {
            n++;
        }
        yypushback(n);
        yybegin(IN_LOCALE_COLON);
        return LOCALE_TOKEN;
            }
          // fall through
          case 107: break;
          case 54:
            // lookahead expression with fixed lookahead length
            zzMarkedPos = Character.offsetByCodePoints
                (zzBufferL, zzMarkedPos, -2);
            { //本地化文件中可以没有，或者有多个locale - 主要是为了兼容localisation/languages.yml
        //locale之前必须没有任何缩进
        //locale之后的冒号和换行符之间应当没有任何字符或者只有空白字符
        //采用最简单的实现方式，尽管JFlex手册中说 "^" "$" 性能不佳
        int n = 1;
        int l = yylength();
        while(Character.isWhitespace(yycharat(l - n))) {
            n++;
        }
        yypushback(n);
        yybegin(IN_LOCALE_COLON);
        return LOCALE_TOKEN;
            }
          // fall through
          case 108: break;
          default:
            zzScanError(ZZ_NO_MATCH);
          }
      }
    }
  }


}
