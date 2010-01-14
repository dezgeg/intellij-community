package com.jetbrains.python.parsing;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.python.psi.LanguageLevel;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class PyParser implements PsiParser {
  private static final Logger LOGGER = Logger.getInstance(PyParser.class.getName());

  private final LanguageLevel myLanguageLevel;

  public PyParser() {
    myLanguageLevel = LanguageLevel.getDefault();
  }

  public PyParser(LanguageLevel languageLevel) {
    myLanguageLevel = languageLevel;
  }

  @NotNull
  public ASTNode parse(IElementType root, PsiBuilder builder) {
    builder.setDebugMode(false);
    long start = System.currentTimeMillis();
    final PsiBuilder.Marker rootMarker = builder.mark();
    ParsingContext context = new ParsingContext(builder, myLanguageLevel);
    StatementParsing stmt_parser = context.getStatementParser();
    builder.setTokenTypeRemapper(stmt_parser); // must be done before touching the caching lexer with eof() call.
    while (!builder.eof()) {
      stmt_parser.parseStatement();
    }
    rootMarker.done(root);
    ASTNode ast = builder.getTreeBuilt();
    long diff = System.currentTimeMillis() - start;
    double kb = builder.getCurrentOffset() / 1000.0;
    LOGGER.debug("Parsed " + String.format("%.1f", kb) + "K file in " + diff + "ms");
    return ast;
  }
}
