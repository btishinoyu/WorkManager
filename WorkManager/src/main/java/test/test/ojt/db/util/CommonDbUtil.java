package test.test.ojt.db.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.test.ojt.common.exception.BusinessException;
import test.test.ojt.common.exception.SystemException;
import test.test.ojt.dao.dto.WorkDto;

public class CommonDbUtil {

	private static final Logger logger = LoggerFactory
			.getLogger(CommonDbUtil.class);

	private static ClassLoader classLoader = CommonDbUtil.class
			.getClassLoader();

	private CommonDbUtil() {
	}

	/**
	 * SQLファイル読み込み
	 * 
	 * @param sqlName
	 *            SQLファイル名
	 * @return 読みこんだSQL文
	 */
	public static StringBuilder readSql(String sqlName) {

		// sqlファイル読みこみ
		InputStream iStream = classLoader
				.getResourceAsStream("/sql/" + sqlName);

		StringBuilder builder = new StringBuilder();
		try (InputStreamReader reader = new InputStreamReader(iStream);
				BufferedReader bufReader = new BufferedReader(reader);) {

			while (true) {
				String line = bufReader.readLine();
				if (line == null) {
					break;
				}
				builder.append(line);
				builder.append("\n");
			}

		} catch (IOException e) {
			logger.error("SQLファイル読み込み失敗", e);
		}
		logger.info("読み込みSQL：{}", builder.toString());
		return builder;
	}

	/**
	 * sql文からパラメータ用Map作成
	 * 
	 * @param sql
	 * @return
	 */
	public static Map<Integer, String> createSqlMap(StringBuilder sql) {
		// sql文の動的パラメータとパラメータの順番のMapを作成
		String regex = "\\$\\{([a-zA-Z\\d]*)\\}";
		Pattern ptm = Pattern.compile(regex);

		// SQL文からパラメータ代入箇所を取得
		HashMap<Integer, String> sqlParamMap = new HashMap<>();
		Matcher mat = ptm.matcher(sql);
		int index = 0;
		while (mat.find()) {
			index++;
			String sqlParam = mat.group(1);

			sqlParamMap.put(index, sqlParam);
		}
		String convertQuery = mat.replaceAll("?");
		// クエリに置換
		sql.replace(0, sql.length(), convertQuery);

		for (Entry<Integer, String> entry : sqlParamMap.entrySet()) {
			logger.info("sqlMap内容[{}]:{}", entry.getKey(), entry.getValue());
		}
		return sqlParamMap;
	}

	/**
	 * @param sql
	 * @param paramMap
	 */
	public static void insertUsers(String sql, Map<Integer, Object> paramMap) {

		DataSource ds = lookup();
		try (Connection con = ds.getConnection();
				PreparedStatement pstm = con.prepareStatement(sql);) {

			logger.info("発行SQL:{}", sql);

			// parameter join
			bindParam(pstm, paramMap);

			int resultCnt = pstm.executeUpdate();
			logger.info("{}件登録", resultCnt);

		} catch (SQLException e) {
			logger.error("DB接続失敗", e);
			throw new SystemException(e);
		}
	}

	// 作業登録処理用
	public static List<WorkDto> findWorking(String sql,
			Map<Integer, Object> paramMap) {

		ArrayList<WorkDto> workDtoList = new ArrayList<>();
		DataSource ds = lookup();
		try (Connection con = ds.getConnection();
				PreparedStatement pstm = con.prepareStatement(sql);) {

			logger.info("発行SQL:{}", sql);

			// parameter join
			bindParam(pstm, paramMap);
			ResultSet result = pstm.executeQuery();

			// マッピング
			while (result.next()) {
				WorkDto workDto = new WorkDto();
				workDto.setId(result.getInt("id"));
				workDto.setStartTime(result.getTime("start_time"));
				workDto.setContents(result.getString("contents"));
				workDto.setNote(result.getString("note"));
				workDtoList.add(workDto);
			}

		} catch (SQLException e) {
			logger.error("DB接続失敗", e);
			throw new SystemException(e);
		}

		return workDtoList;
	}

	/**
	 * 作業終了処理
	 * 
	 * @param string
	 * @param paramMap
	 * @return
	 * @throws BusinessException
	 */
	public static int finishWork(String sql, Map<Integer, Object> paramMap) {

		int resultCnt = 0;
		DataSource ds = lookup();
		try (Connection con = ds.getConnection();
				PreparedStatement pstm = con.prepareStatement(sql)) {

			logger.info("発行SQL:{}", sql);

			// parameter join
			bindParam(pstm, paramMap);
			resultCnt = pstm.executeUpdate();

		} catch (SQLException e) {
			logger.error("DB接続失敗");
			throw new SystemException(e);
		}

		return resultCnt;
	}

	public static int startWork(String sql, Map<Integer, Object> paramMap) {

		DataSource ds = lookup();
		int resultCnt = 0;
		try (Connection con = ds.getConnection();
				PreparedStatement pstm = con.prepareStatement(sql)) {

			bindParam(pstm, paramMap);
			resultCnt = pstm.executeUpdate();

		} catch (SQLException e) {
			logger.error("DB接続失敗", e);
			throw new SystemException(e);
		}

		return resultCnt;
	}

	public static List<WorkDto> findAllWork(String sql,
			Map<Integer, Object> paramMap) {

		DataSource ds = lookup();
		ArrayList<WorkDto> dtoList;
		try (Connection con = ds.getConnection();
				PreparedStatement pstm = con.prepareStatement(sql);) {

			logger.info("発行SQL:{}", sql);

			// bindParam
			bindParam(pstm, paramMap);

			ResultSet result = pstm.executeQuery();

			dtoList = resultSetToWorkDtoList(result);

		} catch (SQLException e) {
			logger.error("DB接続失敗", e);
			throw new SystemException(e);
		}

		return dtoList;
	}

	public static void insertWork(String sql, Map<Integer, Object> paramMap) {

		DataSource ds = lookup();

		try (Connection con = ds.getConnection();
				PreparedStatement pstm = con.prepareStatement(sql);) {

			bindParam(pstm, paramMap);

			int resultCnt = pstm.executeUpdate();
			logger.info("{}件挿入", resultCnt);

		} catch (SQLException e) {
			logger.error("DB接続失敗", e);
			throw new SystemException(e);
		}

	}

	public static LocalTime findTime(String sql, Map<Integer, Object> paramMap,
			String column) throws BusinessException {

		LocalTime time = null;
		DataSource ds = lookup();
		try (Connection con = ds.getConnection();
				PreparedStatement pstm = con.prepareStatement(sql);) {

			logger.info("発行SQL:{}", sql);

			// bindParam;
			bindParam(pstm, paramMap);

			// DB検索
			ResultSet result = pstm.executeQuery();

			int resultcnt = 0;
			while (result.next()) {
				resultcnt++;
				if ("end_time".equals(column)
						&& result.getTime(column) == null) {
					throw new BusinessException("作業中の下に追加はできません。");
				}
				time = result.getTime(column).toLocalTime();
			}
			if (resultcnt != 1) {
				throw new BusinessException("選択した作業の開始または終了時間を取得できませんでした。");
			}

		} catch (SQLException e) {
			logger.error("DB更新失敗", e);
			throw new SystemException(e);
		}

		return time;
	}

	public static void deleteWork(String sql, Map<Integer, Object> paramMap)
			throws BusinessException {

		DataSource ds = lookup();
		try (Connection con = ds.getConnection();
				PreparedStatement pstm = con.prepareStatement(sql);) {

			logger.info("発行SQL:{}", sql);

			// bindParam;
			bindParam(pstm, paramMap);

			int resultCnt = pstm.executeUpdate();

			logger.info("{}件削除フラグ更新", resultCnt);

			if (resultCnt == 0) {
				throw new BusinessException("データは削除されています。");
			} else if (resultCnt > 1) {
				throw new SystemException("削除が正常に行われませんでした。");
			}

		} catch (SQLException e) {
			logger.error("DB更新失敗", e);
			throw new SystemException(e);
		}

	}

	/**
	 * dtoに変換したパラメータをSQL文に補完する。
	 * 
	 * @param pstm
	 *            クエリーSQL文
	 * @param paramMap
	 *            補完用パラメータ
	 * @throws SQLException
	 */
	private static void bindParam(PreparedStatement pstm,
			Map<Integer, Object> paramMap) throws SQLException {

		for (Entry<Integer, Object> entry : paramMap.entrySet()) {

			Object value = entry.getValue();

			if (value instanceof String) {
				pstm.setString(entry.getKey(), (String) entry.getValue());
			} else if (value instanceof Integer) {
				pstm.setInt(entry.getKey(),
						((Integer) entry.getValue()).intValue());
			} else if (value instanceof Time) {
				pstm.setTime(entry.getKey(), (Time) entry.getValue());
			} else if (value instanceof Date) {
				pstm.setDate(entry.getKey(), (Date) entry.getValue());
			} else {
				// 想定外の型は一律String型に置換
				pstm.setString(entry.getKey(), entry.getValue().toString());
			}
		}
	}

	/**
	 * JNDIによりデータソースを取得
	 * 
	 * @return
	 */
	private static DataSource lookup() {

		DataSource ds = null;
		try {
			Context context = new InitialContext();
			ds = (DataSource) context.lookup("java:comp/env/jdbc/postgres");
		} catch (NamingException e) {
			logger.error("JNDI接続エラー:{}", e);
			throw new SystemException(e);
		}
		return ds;
	}

	/**
	 * SQL実行結果をDtoに詰め替える
	 * 
	 * @param result
	 * @return
	 * @throws SQLException
	 */
	private static ArrayList<WorkDto> resultSetToWorkDtoList(ResultSet result)
			throws SQLException {

		String regex = "_[a-z]";
		Pattern ptm = Pattern.compile(regex);

		HashMap<String, String> clmNameMap = new HashMap<String, String>();
		ResultSetMetaData meta = result.getMetaData();
		for (int i = 1; i < meta.getColumnCount() + 1; i++) {

			meta.getColumnType(i);
			clmNameMap.put(meta.getColumnClassName(i), meta.getColumnLabel(i));
		}

		ArrayList<WorkDto> dtoList = new ArrayList<>();
		// Method[] dtoMethods = new WorkDto().getClass().getMethods();
		HashMap<String, String> setterTypeMap = new HashMap<String, String>();
		for (Entry<String, String> entry : clmNameMap.entrySet()) {
			String clmNm = entry.getValue();
			logger.info("clmNameMap内容[型]:ラベル     [{}]:{}", entry.getKey(),
					clmNm);

			// ラベル名からフィールド名へ変換
			Matcher mat = ptm.matcher(clmNm);
			if (mat.find()) {
				clmNm = clmNm.replace(mat.group(),
						mat.group().substring(1).toUpperCase());
			}
			String setter = "set" + clmNm.substring(0, 1).toUpperCase()
					+ clmNm.substring(1);
			logger.info("setter:{}", setter);
			clmNameMap.replace(entry.getKey(), setter);

		}
		Object obj;
		for (Entry<String, String> entry : clmNameMap.entrySet()) {
			logger.info("clmNameMap内容[型]:setter     [{}]:{}", entry.getKey(),
					entry.getValue());
			try {
				obj = Class.forName(entry.getValue()).getClass();
			} catch (ClassNotFoundException e) {
				logger.error("DBから取得した値の変換に失敗しました。");
				throw new SystemException(e);
			}
		}

		while (result.next()) {
			WorkDto dto = new WorkDto();

			dto.setId((result.getInt("id")));
			dto.setStartTime(result.getTime("start_time"));
			dto.setEndTime(result.getTime("end_time"));
			dto.setWorkingTime(result.getTime("working_time"));
			dto.setContents(result.getString("contents"));
			dto.setNote(result.getString("note"));
			dtoList.add(dto);
		}

		return dtoList;
	}

}
