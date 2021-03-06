package com.ace.acemanager.service.rental;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.ace.acemanager.common.Constants;
import com.ace.acemanager.dao.rental.basic.RentalCommunityMapper;
import com.ace.acemanager.dao.rental.basic.RentalHouseMapper;
import com.ace.acemanager.dao.rental.basic.RentalHouseUserMapper;
import com.ace.acemanager.dao.rental.basic.RentalRoomMapper;
import com.ace.acemanager.dao.rental.contract.RentalContractMapper;
import com.ace.acemanager.pojo.RentalCommunity;
import com.ace.acemanager.pojo.RentalContract;
import com.ace.acemanager.pojo.RentalHouse;
import com.ace.acemanager.pojo.RentalHouseUser;
import com.ace.acemanager.pojo.RentalRoom;
import com.ace.acemanager.pojo.User;

/**
 * 房源基础业务Service接口实现类
 *
 * @author Liuqi
 * 2017-7-11
 */
@Service
public class RentalBasicServiceImpl implements RentalBasicService {

    private Logger logger = Logger.getLogger(RentalBasicServiceImpl.class);

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    @Resource
    private RentalHouseMapper rentalHouseMapper;
    @Resource
    private RentalRoomMapper rentalRoomMapper;
    @Resource
    private RentalContractMapper rentalContractMapper;
    @Resource
    private RentalCommunityMapper rentalCommunityMapper;
    @Resource
    private RentalHouseUserMapper rentalHouseUserMapper;

    @Override
    public List<RentalCommunity> getCommunityListByUserId(RentalCommunity community) throws Exception {
        return rentalCommunityMapper.getCommunityListByUserId(community);
    }

    @Override
    public List<RentalHouse> getHouseListByCommunityIdAndUserId(Integer userId, Integer communityId) throws Exception {
        return rentalHouseMapper.getHouseListByCommunityIdAndUserId(userId, communityId);
    }

    @Override
    public RentalHouse getHouseDetailByHouseId(RentalHouse house) throws Exception {
        return rentalHouseMapper.getHouseDetailByHouseId(house);
    }

    @Override
    public List<RentalRoom> getRoomsByHouseId(RentalRoom room) throws Exception {
        return rentalRoomMapper.getRoomsByHouseId(room);
    }

    @Override
    public Integer getOnrentRoomCount(RentalHouse house) throws Exception {
        return rentalRoomMapper.getOnrentRoomCount(house);
    }

    @Override
    public Integer count(RentalRoom room) throws Exception {
        return rentalRoomMapper.count(room);
    }

    /**
     * 增加小区
     * 返回所增加的小区主键ID
     *
     * @param community
     * @return
     */
    private Integer addCommunity(RentalCommunity community) {
        community.setCreateTime(new Date());
        community.setModifiedTime(community.getCreateTime());
        int commRowCount = rentalCommunityMapper.addCommunity(community);
        if (commRowCount == 1) {//增加小区成功
            Integer commId = community.getId();//所增加小区的主键ID
            logger.debug("增加   " + commId + " 号小区成功");
            return commId;
        } else {
            return null;
        }
    }

    /**
     * 纯粹增加房源
     *
     * @param house
     * @return
     */
    private boolean addSingleHouse(RentalHouse house) {
        boolean flag = true;
        house.setCreateTime(house.getCommunity().getCreateTime());
        house.setModifiedTime(house.getCommunity().getCreateTime());
        house.setInRenovation("on".equals(house.getInRenovation()) ? Constants.RENTAL_HOUSE_IN_RENOVEATION : Constants.RENTAL_HOUSE_OUT_RENOVEATION);
        house.setRoomCount("整租".equals(house.getRentalType()) ? Constants.RENTAL_ROOM_DEFAULTCOUNT : house.getRoomCount());
        house.setStatus(Constants.RENTAL_HOUSE_USERABLE);
        house.setIsDelete(0);
        int houseRowCount = rentalHouseMapper.addHouse(house);
        if (houseRowCount == 1) {
            //增加房源成功
            Integer houseId = house.getId();
            logger.debug("增加   " + houseId + " 号房源成功");

			/*
             * 同步更新房源用户关系多对多表
			 */
            //新增  "当前登录ID -- 所添加房源ID" 关系链
            RentalHouseUser houseUser = new RentalHouseUser();
            houseUser.setUserId(house.getCreateUserId());
            houseUser.setHouseId(houseId);
            houseUser.setCreateTime(house.getCommunity().getCreateTime());
            houseUser.setModifiedTime(house.getCommunity().getCreateTime());
            int hurRowCount = rentalHouseUserMapper.addHUR(houseUser);
            //FIXME 已修复  倘若当前用户是主账号，那么    新增  "当前登录账号的主账号ID -- 所添加房源ID" 关系链
            if (house.getCreateUserId() != house.getManagerUserId()) {
                RentalHouseUser houseUser2 = new RentalHouseUser();
                houseUser2.setUserId(house.getManagerUserId());
                houseUser2.setHouseId(houseId);
                houseUser2.setCreateTime(house.getCommunity().getCreateTime());
                houseUser2.setModifiedTime(house.getCommunity().getCreateTime());
                rentalHouseUserMapper.addHUR(houseUser2);
            }
            if (hurRowCount == 1) {
                logger.debug("维护房源用户多对多关系表成功");
            } else {
                flag = false;
            }
            //3、增加房间
            RentalRoom room = new RentalRoom();
            room.setHouseId(houseId);
            room.setCreateTime(house.getCommunity().getCreateTime());
            room.setModifiedTime(house.getCommunity().getCreateTime());
            room.setStatus(Constants.RENTAL_ROOM_STATUS_EMPTY);
            room.setIsDelete(0);
            for (int i = 0; i < house.getRoomCount(); i++) {
                room.setRoomName(Constants.RENTAL_ROOM_DEFAULTNAME + (i + 1));
                int roomRowCount = rentalRoomMapper.addRoom(room);
                if (roomRowCount == 1) {
                    logger.debug("增加房间成功");
                } else {
                    logger.debug("增加房间失败");
                    flag = false;
                }
            }
        } else {
            flag = false;
        }
        return flag;
    }

    @Override
    public String aceAddHouse(RentalHouse house) throws Exception {
        String result = "";
        RentalCommunity comm = house.getCommunity();
        comm.setCreateTime(new Date());
        comm.setModifiedTime(comm.getCreateTime());

        RentalCommunity _comm = rentalCommunityMapper.getCommunityByAddressAndName(comm);
        if (null != _comm) {
            //此房源所处的小区已经存在了
            house.setCommunityId(_comm.getId());
            RentalHouse _house = rentalHouseMapper.getHouseByCommIdAndHouseAddress(house);
            if (null != _house) {//此房源已存在
                result = "房源已存在,添加失败!";
            } else {
                //在已存在的小区里添加房源
                if (addSingleHouse(house)) {
                    result = "增加房源成功!";
                }
            }
        } else {
            //添加新小区和新房源
            Integer commId = addCommunity(comm);
            house.setCommunityId(commId);
            if (addSingleHouse(house)) {
                result = "增加房源成功!";
            }
        }
        return result;
    }

    @Override
    public List<RentalRoom> getRoomsForRoomInfo(RentalHouse house) throws Exception {
        return rentalRoomMapper.getRoomsForRoomInfo(house);
    }

    @Override
    public boolean aceUpdateHouse(RentalHouse house) throws Exception {
        //根据前台传来的houseId,查出持久化小区对象的id
        Integer _commId = rentalHouseMapper.getHouseById(house.getId()).getCommunityId();

        //1、更新房源所在小区信息
        RentalCommunity community = house.getCommunity();
        community.setId(_commId);
        community.setModifiedTime(new Date());
        if (null == community.getArea()) {//解决直辖区 区域不置空的BUG
            community.setArea(null);
        }
        int commRowCount = rentalCommunityMapper.updateCommunityById(community);
        if (commRowCount == 1) {
            logger.debug("更新    " + _commId + " 号小区成功!");
            //2、更新房源信息
            house.setModifiedTime(community.getModifiedTime());
            if (Constants.RENTAL_HOUSE_OUT_RENOVEATION.equals(house.getInRenovation())) {// 未装修
                house.setDecorationStartTime(null);
                house.setDecorationEndTime(null);
            }
            int houseRowCount = rentalHouseMapper.updateHouseById(house);
            if (houseRowCount == 1) {
                logger.debug("更新    " + house.getId() + " 号房源成功!");
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean aceAddRoomForHouse(RentalRoom room) throws Exception {
        List<String> roomNames = rentalRoomMapper.getRoomNameListByHouseId(room.getHouseId());
        //算出最新的默认房间名
        String latestRoomName = getLatestDefaultRoomName(roomNames);
        room.setRoomName(latestRoomName);
        room.setStatus(Constants.RENTAL_ROOM_STATUS_EMPTY);
        room.setCreateTime(new Date());
        room.setModifiedTime(room.getCreateTime());
        room.setIsDelete(0);
        //增加房间
        int rowCount = rentalRoomMapper.addRoom(room);
        if (rowCount == 1) {
            //同步更新house表中的字段roomCount,房间数量+1
            RentalHouse _house = rentalHouseMapper.getHouseById(room.getHouseId());
            _house.setRoomCount(_house.getRoomCount() + 1);
            rentalHouseMapper.updateHouseById(_house);

            logger.debug("增加房间成功!");
            return true;
        } else {
            logger.debug("增加房间失败!");
            return false;
        }
    }

    /**
     * 判断一个字符串是否是整数
     *
     * @param str
     * @return
     */
    private static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
    }

    /**
     * 根据指定房间列表获取下一个默认房间名
     * 新增的房间名是默认房间名，其后缀号码应该是当前房源下默认房间的最大后缀号码+1
     * 举例："新的房间4"==> "新的房间5" ==>"新的房间6" ....
     *
     * @param roomNames
     * @return
     */
    private String getLatestDefaultRoomName(List<String> roomNames) {
        int max = 0;
        for (String rname : roomNames) {
            if (null != rname && rname.toString().startsWith(Constants.RENTAL_ROOM_DEFAULTNAME)) {
                //以"新的房间"开头
                String suffix = rname.substring(Constants.RENTAL_ROOM_DEFAULTNAME.length());
                if (isNumeric(suffix)) {
                    //除去"新的房间"之后的内容是数字,找到最大值
                    int temp = Integer.parseInt(suffix);
                    max = temp > max ? temp : max;
                }
            }
        }
        return Constants.RENTAL_ROOM_DEFAULTNAME + (max + 1);
    }

    @Override
    public RentalRoom getRoomDetailByRoomId(Integer roomId) throws Exception {
        return rentalRoomMapper.getRoomDetailByRoomId(roomId);
    }

    @Override
    public boolean canDisableHouse(Integer houseId) throws Exception {
		/*
		 * 能停用房源的规则:该房源下的所有房间都没有生效的租客合同时,才能停用
		 *  即  有房间在出租中，不能停
		 */
        return rentalHouseMapper.getEffectRenterContractsCountByHouseId(houseId) == 0;
    }

    @Override
    public String canDeleteHouse(Integer houseId) throws Exception {
		/*
		 * 能删除房源的规则:该房源和其下的所有房间均没有生效的业主合同和租客合同时,才能删除(可逆删)
		 * 	即  有房间出租 不能删， 房源已出租，不能删
		 */
        if (rentalHouseMapper.getEffectOwnnerContractsCountByHouseId(houseId) > 0) {
            return "该房源已出租,不能删除!";
        } else if (rentalHouseMapper.getEffectRenterContractsCountByHouseId(houseId) > 0) {
            return "该房源下有房间在出租中,不能删除!";
        }
        return "allowed_del_House";
    }

    @Override
    public boolean aceDisableHouse(Integer houseId) throws Exception {
        //先停用该房源
        int rowCount = rentalHouseMapper.changeHouseStatus(houseId, Constants.RENTAL_HOUSE_STOPUSER);
        if (rowCount == 1) {
            //再停用该房源下的所有房间
            rentalRoomMapper.changeRoomsStatus(houseId, Constants.RENTAL_ROOM_STATUS_STOPUSE);
            return true;
        }
        return false;
    }

    @Override
    public boolean aceAbleHouse(Integer houseId) throws Exception {
        //先启用该房源
        int rowCount = rentalHouseMapper.changeHouseStatus(houseId, Constants.RENTAL_HOUSE_USERABLE);
        if (rowCount == 1) {
            //再启用该房源下的所有房间
            rentalRoomMapper.changeRoomsStatus(houseId, Constants.RENTAL_ROOM_STATUS_EMPTY);
            return true;
        }
        return false;
    }

    //XXX 在可逆删除房源、房间时，均没有去更新 "修改时间" 字段

    @Override
    public boolean aceDeleteHouse(Integer houseId) throws Exception {
        //先删除该房源
        int rowCount = rentalHouseMapper.deleteHouseByIdReversibly(houseId);
        if (rowCount == 1) {
            //再删除该房源下的所有房间
            rentalRoomMapper.deleteRoomsByHouseIdReversibly(houseId);
            return true;
        }
        return false;
    }

    @Override
    public boolean aceDeleteRoom(Integer roomId) throws Exception {
        //先更新此房间所在房源表下的房间数量 - 1
        RentalHouse house = rentalHouseMapper.getHouseById(rentalRoomMapper.getRoomById(roomId).getHouseId());
        house.setRoomCount(house.getRoomCount() - 1);
        int rowCount = rentalHouseMapper.updateHouseById(house);
        if (rowCount == 1) {
            //再删除此房间
            rentalRoomMapper.deleteRoomByIdReversibly(roomId);
            return true;
        }
        return false;
    }

    @Override
    public boolean modifyRoomByRoomId(RentalRoom room) throws Exception {
        return rentalRoomMapper.updateRoomById(room) == 1;
    }

    @Override
    public void addTipsForRoom(RentalRoom room) throws Exception {
        if (null != room) {
            switch (room.getStatus()) {
                case Constants.RENTAL_ROOM_STATUS_EMPTY:
                    //房间当前状态:"空置"
                    Date lastAbandanDate = rentalRoomMapper.getLastAbandanDateByRoomId(room.getId());
                    long emptyDays = 0L;
                    if (null == lastAbandanDate) {//此房间从未出租过
                        emptyDays = (new Date().getTime() - room.getCreateTime().getTime()) / Constants.DAY_MS;
                    } else { //此房间有人住过
                        emptyDays = (new Date().getTime() - lastAbandanDate.getTime()) / Constants.DAY_MS;
                    }
                    room.setTips("已空置 " + emptyDays + " 天");
                    break;
                case Constants.RENTAL_ROOM_STATUS_RENTED:
                    //房间当前状态:"已租"
                    Date fristReceiptDate = rentalRoomMapper.getFristReceiptDateByRoomId(room.getId());
                    if (null != fristReceiptDate) {
                        room.setTips("收租：" + format.format(fristReceiptDate));
                    } else {
                        RentalContract contract = rentalContractMapper.getContractsByRoomIdAndStatus(room.getId(), Constants.RENTAL_CONTRACT_EFFECTIVE).get(0);
                        room.setTips("合同到期：" + format.format(contract.getEndDate()));
                    }
                    break;
                case Constants.RENTAL_ROOM_STATUS_STOPUSE:
                    //房间当前状态:"停用"
                    room.setTips("所处房源停用中");
                    break;
                default:
                    room.setTips("");
                    break;
            }
        } else {
            throw new Exception("所要添加温馨提示信息的房间不存在");
        }
    }

    @Override
    public void setFirstPayingBillIdForRoom(RentalRoom room) throws Exception {
        Integer firstPayingBillId = rentalRoomMapper.getFirstPayingBillIdByRoomId(room.getId());
        room.setFirstPayingBillId(firstPayingBillId);
    }

    @Override
    public boolean canDeleteRoom(Integer roomId) throws Exception {
        return rentalContractMapper.getContractsByRoomIdAndStatus(roomId, Constants.RENTAL_CONTRACT_EFFECTIVE).size() == 0;
    }

    /**
     * 省市级联JSON转换
     *
     * @param map
     * @return
     */
    private String convertPCCascadeToJson(Map<String, List<String>> map) {
        StringBuffer sb = new StringBuffer("{\"citylist\":[{\"p\":\"不限\",\"c\":[{\"n\":\"不限\"}]},");
        Set<String> key = map.keySet();
        for (String pro : key) {
            sb.append("{\"p\":\"" + pro + "\",\"c\":[");
            for (String city : map.get(pro)) {
                sb.append("{\"n\":\"" + city + "\"},");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]},");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public String getProAndCityCascade(User user) throws Exception {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        List<String> tempList;

        RentalCommunity community = new RentalCommunity();
        community.setUserId(user.getId());

        List<RentalCommunity> allProAndCitys = rentalCommunityMapper.getAllProAndCityFromComm(community);

        //遍历所有省市
        for (RentalCommunity comm : allProAndCitys) {
            String province = comm.getProvince();
            String city = comm.getCity();
            tempList = new ArrayList<String>();
            if (!map.containsKey(province)) {//一个新的key
                tempList.add(city);
                map.put(province, tempList);
            } else {
                map.get(province).add(city);
            }
        }
        return convertPCCascadeToJson(map);
    }

    @Override
    public Integer getRoomCountByRoomStatus(String status, Integer userId) throws Exception {
        return rentalRoomMapper.getRoomCountByRoomStatus(status, userId);
    }

}
