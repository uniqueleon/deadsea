package org.aztec.deadsea.sql.impl;

import org.aztec.deadsea.sql.GenerationParameter;
import org.aztec.deadsea.sql.ShardingConfiguration;
import org.aztec.deadsea.sql.ShardingSqlGenerator;

public class QuerySqlGenerator implements ShardingSqlGenerator {

	public QuerySqlGenerator() {
		// TODO Auto-generated constructor stub
	}

	public String generate(GenerationParameter param) {
		StringBuilder builder = new StringBuilder(param.getRawSql());
		String rawSql = param.getRawSql();
		String newSql = rawSql;
		int dbSize = conf.getDataBaseNum();
		for(int i = 0;i < dbSize;i++) {
			for(int j = 0;j < param.getGeneratedTableNames().length;j++){
				if(builder.length() == 0) {
					builder.append(" UNION ALL ");
				}
				String tableName = param.getGeneratedTableNames()[j];
				String dbName = noDbSelected ? "" : param.getGeneratedDbNames()[i];
				if(!noDbSelected) {
					builder.append(rawSql.replace(param.getDbName(), dbName));
				}
				builder.append(rawSql.replace(param.getTableName(), tableName));
			}
		}
		return builder.toString();
	}

}
