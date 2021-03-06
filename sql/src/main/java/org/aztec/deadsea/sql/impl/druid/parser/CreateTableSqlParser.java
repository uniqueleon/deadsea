package org.aztec.deadsea.sql.impl.druid.parser;

import org.aztec.deadsea.sql.ShardingSqlException;
import org.aztec.deadsea.sql.SqlType;
import org.aztec.deadsea.sql.impl.druid.DruidMetaData;
import org.aztec.deadsea.sql.impl.druid.DruidSqlParser;
import org.springframework.stereotype.Component;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;

@Component
public class CreateTableSqlParser implements DruidSqlParser {

	public CreateTableSqlParser() {
		// TODO Auto-generated constructor stub
	}

	public boolean accept(SQLStatement sql) {
		return sql instanceof SQLCreateTableStatement;
	}

	public DruidMetaData parse(SQLStatement sql) throws ShardingSqlException {
		
		SQLCreateTableStatement scts = (SQLCreateTableStatement) sql;
		DruidMetaData mData = ParserHelper.getMetaData(sql.toString(), scts.getTableSource());
		mData.setType(SqlType.CREATE_TABLE);
		return mData;
	}

}
