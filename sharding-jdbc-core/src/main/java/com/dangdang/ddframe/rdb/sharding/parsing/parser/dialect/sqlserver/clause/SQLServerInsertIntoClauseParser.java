package com.dangdang.ddframe.rdb.sharding.parsing.parser.dialect.sqlserver.clause;

import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.LexerEngine;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.clause.InsertIntoClauseParser;

/**
 * SQLServer的INSERT INTO从句解析器.
 *
 * @author zhangliang
 */
public final class SQLServerInsertIntoClauseParser extends InsertIntoClauseParser {
    
    public SQLServerInsertIntoClauseParser(final ShardingRule shardingRule, final LexerEngine lexerEngine) {
        super(lexerEngine, new SQLServerTableReferenceClauseParser(shardingRule, lexerEngine));
    }
}