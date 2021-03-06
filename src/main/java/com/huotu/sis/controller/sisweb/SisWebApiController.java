package com.huotu.sis.controller.sisweb;

import com.huotu.huobanplus.common.entity.Order;
import com.huotu.huobanplus.common.entity.OrderItems;
import com.huotu.huobanplus.common.entity.User;
import com.huotu.sis.entity.SisConfig;
import com.huotu.sis.model.sisweb.ResultModel;
import com.huotu.sis.repository.SisConfigRepository;
import com.huotu.sis.repository.SisInviteRepository;
import com.huotu.sis.repository.SisRepository;
import com.huotu.sis.repository.mall.OrderItemsRepository;
import com.huotu.sis.repository.mall.OrderRepository;
import com.huotu.sis.repository.mall.UserRepository;
import com.huotu.sis.service.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Objects;

/**
 * 店中店外部调用接口
 * Created by slt on 2016/2/18.
 */
@Controller
@RequestMapping("/sisapi")
public class SisWebApiController {
    private static Log log = LogFactory.getLog(SisWebApiController.class);
    @Autowired
    SisInviteRepository sisInviteRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository sisOrderRepository;

    @Autowired
    private OrderItemsRepository sisOrderItemsRepository;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserService userService;

    @Autowired
    private SisService sisService;

    @Autowired
    private SisRepository sisRepository;

    @Autowired
    private SisConfigRepository sisConfigRepository;

    @Autowired
    private SisLevelService sisLevelService;

    @Autowired
    private Environment environment;

    @Autowired
    private SisConfigService sisConfigService;

    /**
     * 检查签名是否正确
     * @param request   请求
     * @return          签名是否正确
     */
    private boolean checkSign(HttpServletRequest request){
        if(!environment.acceptsProfiles("develop")&&!environment.acceptsProfiles("development")&&!environment.acceptsProfiles("staging")){
            String sign=request.getParameter("sign");
            try {
                if (sign == null || !sign.equals(securityService.getSign(request))) {
                    return false;
                }
            } catch (UnsupportedEncodingException e) {
                log.debug("签名解析异常");
                return false;
            }
        }
        return true;

    }

    /**
     * 升级用户店中店
     * 此接口在需求中以不在使用
     *
     * <b>负责人：史利挺</b>
     * @return              升级结果
     * @throws Exception    异常
     */
    @RequestMapping(value = "/upgradeSisShop",method = {RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public ResultModel upgradeSisShop(HttpServletRequest request) throws Exception {
        log.debug("upgradeSisShop");
        ResultModel resultModel=new ResultModel();
        //第一步:参数有效性判断
        boolean isNotTrueSign=checkSign(request);
        if(!isNotTrueSign){
            resultModel.setCode(401);
            resultModel.setMessage("授权失败：签名未通过！");
            return resultModel;
        }
        String userId=request.getParameter("userid");
        if(StringUtils.isEmpty(userId)){
            resultModel.setCode(403);
            resultModel.setMessage("参数错误：没有用户ID！");
            return resultModel;
        }
        User user = userRepository.findOne(Long.parseLong(userId));
        if(Objects.isNull(user)){
            resultModel.setCode(403);
            resultModel.setMessage("参数错误：找不到用户！");
            return resultModel;
        }

        SisConfig sisConfig=sisConfigRepository.findByMerchantId(user.getMerchant().getId());
        if(sisConfig==null||sisConfig.getEnabled()==0||sisConfig.getOpenGoodsMode()==null||sisConfig.getOpenGoodsMode()==0){
            resultModel.setCode(403);
            resultModel.setMessage(userId+"商户无店中店配置不是升级的条件");
            return resultModel;
        }
        String orderId=request.getParameter("orderid");
        List<OrderItems> orderItems=sisOrderItemsRepository.getOrderItemsByOrderId(orderId);
        if(orderItems==null||orderItems.isEmpty()){
            resultModel.setCode(403);
            resultModel.setMessage(orderId+"订单无效！");
            return resultModel;

        }

        log.debug("user:"+userId+",upgradeSisShopOverOrderid:"+orderId);
        //第二步:补差价升级
        boolean isUpgrade=sisLevelService.upgradeSisLevel(user,sisConfig,orderItems.get(0));

        //第三步:返回结果
        if(isUpgrade){
            resultModel.setCode(200);
            resultModel.setMessage("OK");
        }else {
            resultModel.setCode(500);
            resultModel.setMessage("Fail");
            log.debug("user"+userId+"Upgrade failed");

        }
        return resultModel;
    }


    /**
     * 开店操作，具体步骤：
     * 1.开启店中店
     * 2.用户返利
     * 3.合伙人送股
     * 4.升级
     *
     * @param request       request请求
     * @return              开店结果
     * @throws Exception    异常
     */
    @RequestMapping(value = "/openSisShop",method = {RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public ResultModel open(HttpServletRequest request) throws Exception {
        log.debug("into openShop");
        ResultModel resultModel=new ResultModel();

        //第一步:参数有效性判断
        boolean isNotTrueSign=checkSign(request);
        if(!isNotTrueSign){
            resultModel.setCode(401);
            resultModel.setMessage("授权失败：签名未通过！");
            return resultModel;
        }

        //参数验证
        String userId=request.getParameter("userid");
        if(StringUtils.isEmpty(userId)){
            resultModel.setCode(403);
            resultModel.setMessage("参数错误：没有用户ID！");
            return resultModel;
        }

        User user = userRepository.findOne(Long.parseLong(userId));
        if(Objects.isNull(user)){
            resultModel.setCode(403);
            resultModel.setMessage("参数错误：找不到用户！");
            return resultModel;
        }

//        Sis sis = sisRepository.findByUser(user);
//        if(sis!=null){
//            resultModel.setCode(500);
//            resultModel.setMessage(userId+"店中店已经开启");
//            return resultModel;
//        }

        //兼容
        sisConfigService.compatibilityOpenShopGoodsAndSelected(user.getMerchant().getId());

        SisConfig sisConfig=sisConfigRepository.findByMerchantId(user.getMerchant().getId());
        if(sisConfig==null){
            resultModel.setCode(500);
            resultModel.setMessage(userId+"商户没有店中店配置信息");
            return resultModel;
        }

        String orderId=request.getParameter("orderid");
        String unionorderId=request.getParameter("unionorderid");
        log.info("userId:"+userId+"orderID="+orderId+"into openShop");
        //开店
        userService.newOpen(user,orderId,sisConfig);

        //新开店奖计算
        userService.newCountOpenShopAward(user, orderId, unionorderId,sisConfig);

        //新合伙人送股
        userService.givePartnerStock(user, orderId,sisConfig);

        //上线升级
        sisLevelService.upgradeAllSisLevel(user,sisConfig);

        resultModel.setCode(200);
        resultModel.setMessage("OK");
        return resultModel;
    }


    /**
     * 计算直推奖
     * @param httpServletRequest    request请求
     * @return                      返回计算结果
     * @throws Exception            抛出异常
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/calculateShopRebate", method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public ResultModel calculateShopRebate(HttpServletRequest httpServletRequest) throws Exception {

        ResultModel resultModel = new ResultModel();
        //第一步:参数有效性判断
        boolean isNotTrueSign=checkSign(httpServletRequest);
        if(!isNotTrueSign){
            resultModel.setCode(401);
            resultModel.setMessage("授权失败：签名未通过！");
            return resultModel;
        }

        String shopIdString=httpServletRequest.getParameter("shopid");
        if(StringUtils.isEmpty(shopIdString)){
            resultModel.setCode(402);
            resultModel.setMessage("参数错误：没有shopId！");
            return resultModel;
        }

        Long shopId=Long.parseLong(shopIdString);
        String orderId=httpServletRequest.getParameter("orderid");
        if(StringUtils.isEmpty(orderId)){
            resultModel.setCode(403);
            resultModel.setMessage("参数错误：没有orderId！");
            return resultModel;
        }

        String unionOrderId=httpServletRequest.getParameter("unionorderid");
        if(StringUtils.isEmpty(unionOrderId)){
            resultModel.setCode(404);
            resultModel.setMessage("参数错误：没有unionorderId！");
            return resultModel;
        }

        User user=userRepository.findOne(shopId);//店主(会员)
        if(Objects.isNull(user)){
            resultModel.setCode(500);
            resultModel.setMessage("未找到店主");
            return resultModel;
        }

        Order order = sisOrderRepository.findOne(orderId);
        if(Objects.isNull(order)){
            resultModel.setCode(500);
            resultModel.setMessage("未找到订单");
            return resultModel;
        }

        SisConfig sisConfig=sisConfigRepository.findByMerchantId(user.getMerchant().getId());
        if(Objects.isNull(sisConfig)||sisConfig.getEnabled()==0){
            resultModel.setCode(500);
            resultModel.setMessage("未找到店铺配置或店铺已关闭");
            return resultModel;
        }
        log.info("userId:"+user.getId()+"orderID="+orderId+"into calculateShopRebate");

        sisService.calculatePushAward(user,order,unionOrderId,sisConfig);

        resultModel.setCode(200);
        resultModel.setMessage("OK");
        return resultModel;
    }

}
