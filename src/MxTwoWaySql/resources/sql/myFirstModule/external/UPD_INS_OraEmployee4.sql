-- @EXT_DB@ 

INSERT INTO EMPLOYEE4
(
 ID, NAME, ADDRESS, IS_HOGE, BIRTH_DATE, INT_VAL, TRAN_TIME, TRAN_TIME_WITH_TZ, TRAN_TIME_WITH_LTZ
) VALUES (
    (select MAX(ID)+1 from EMPLOYEE4),
    /*pmb.Name*/'タイムゾーンテスト',
    /*pmb.Address*/'明石市',
    /*pmb.IS_HOGE*/TRUE,
    /*pmb.BIRTH_DATE*/'2021/01/02 0:00:00',
    /*pmb.INT_VAL*/234,
    /*pmb.TRAN_TIME*/'2022/01/02 0:00:00',
    /*pmb.TRAN_TIME_WITH_TZ*/'23-01-02 00:00:00.000000 +10:00',
    /*pmb.TRAN_TIME_WITH_LTZ*/'24-01-02 00:00:00.000000'
)
/*END*/