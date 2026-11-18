# Offline translation test, EN ⇄ 中 (18 Nov 2026)

45 real-life sentences (25 Chinese signs/notices/messages, 20 English traveller phrases). Score per sentence: 2 good, 1 understandable but awkward, 0 wrong or misleading. Max 50 (中→EN), 40 (EN→中), 90 total.

| Engine | 中→EN | EN→中 | Total | Size on phone |
|---|---|---|---|---|
| ML Kit (now) | 26/50 | 31/40 | **57/90** | ~30 MB / language |
| Bergamot (Firefox) | 29/50 | 30/40 | **59/90** | ~50 MB / direction (~20 MB download) |
| Qwen2.5 1.5B | 43/50 | 38/40 | **81/90** | ~1 GB (both directions) |
| Qwen2.5 3B | 42/50 | 37/40 | **79/90** | ~1.9 GB |

## Every sentence

**1. 请勿吸烟**

- ML Kit (now) `2`: No smoking
- Bergamot (Firefox) `2`: Do not smoke.
- Qwen2.5 1.5B `2`: No smoking.
- Qwen2.5 3B `2`: No Smoking
- Qwen2.5 14B (reference, not phone-size): No Smoking

**2. 小心地滑**

- ML Kit (now) `2`: Caution: Slippery!
- Bergamot (Firefox) `0`: Slide carefully.
- Qwen2.5 1.5B `2`: Be careful of the slippery floor.
- Qwen2.5 3B `2`: Caution: Wet Floor
- Qwen2.5 14B (reference, not phone-size): Caution: Slippery Floor

**3. 出口在左手边，请走楼梯。**

- ML Kit (now) `1`: Export is on the left hand, please take the stairs.
- Bergamot (Firefox) `2`: Exit on the left side, please take the stairs.
- Qwen2.5 1.5B `2`: Exit to your left; take the stairs.
- Qwen2.5 3B `2`: The exit is on your left, please use the stairs.
- Qwen2.5 14B (reference, not phone-size): The exit is on the left, please use the stairs.

**4. 本店只收微信和支付宝，不收现金。**

- ML Kit (now) `1`: This shop only receives WeChat and Alipay, no cash.
- Bergamot (Firefox) `2`: The store only accepts WeChat and Alipay, and does not accept cash.
- Qwen2.5 1.5B `2`: This store accepts only WeChat and Alipay; cash is not accepted.
- Qwen2.5 3B `2`: This store only accepts WeChat and Alipay payments, no cash.
- Qwen2.5 14B (reference, not phone-size): This store only accepts WeChat and Alipay payments, no cash.

**5. 列车即将进站，请站在黄线以外候车。**

- ML Kit (now) `0`: The train is about to enter, please stand in the yellow line.
- Bergamot (Firefox) `1`: The train is about to enter the station, please stand outside the Yellow Line waiting.
- Qwen2.5 1.5B `2`: Please stand outside the yellow line when the train is about to arrive.
- Qwen2.5 3B `2`: The train is approaching the station, please stand outside the yellow line to wait.
- Qwen2.5 14B (reference, not phone-size): The train will be arriving shortly. Please wait behind the yellow line.

**6. 退票须在开车前两小时办理，否则按规定收取手续费。**

- ML Kit (now) `1`: The refund must be handled two hours before driving, otherwise charge fee is charged according to the regulations.
- Bergamot (Firefox) `1`: The refund must be processed two hours before driving, otherwise the fee is charged as required.
- Qwen2.5 1.5B `1`: You must cancel your ticket at least two hours before departure; otherwise, you will be charged for the fare.
- Qwen2.5 3B `2`: Ticket refunds must be processed two hours before the departure time; otherwise, a refund fee will be charged according to regulations.
- Qwen2.5 14B (reference, not phone-size): Refunds must be processed more than two hours before departure; otherwise, a handling fee will be charged according to regulations.

**7. 麻辣香锅（微辣、中辣、特辣可选）**

- ML Kit (now) `1`: Spicy incense pot (slightly spicy, spicy, spicy)
- Bergamot (Firefox) `0`: Spy fragrant pot (micro-spicy, medium spicy, special spity optional)
- Qwen2.5 1.5B `1`: Spicy Hot Pot (Micro, Medium, and Extra Spicy Available)
- Qwen2.5 3B `1`: Mala Pot Pot (choose from mild, medium hot, spicy)
- Qwen2.5 14B (reference, not phone-size): Spicy Hot Pot (Mild Spicy, Medium Spicy, and Extra Spicy available)

**8. 宫保鸡丁盖饭 28元**

- ML Kit (now) `0`: Palace, Chicken, Lint, 28 yuan
- Bergamot (Firefox) `0`: The chicken is $28.
- Qwen2.5 1.5B `1`: Kung Pao Chicken Fried Rice 28 Yuan
- Qwen2.5 3B `2`: Kung Pao Rice Bowl with Fried Chicken 28 yuan
- Qwen2.5 14B (reference, not phone-size): Kung Pao Chicken Rice Bowl 28 yuan

**9. 本品含有花生，过敏者慎用。**

- ML Kit (now) `1`: This product contains peanuts, allergies caution.
- Bergamot (Firefox) `1`: This product contains peanuts, and allers are careful.
- Qwen2.5 1.5B `2`: This product contains peanuts; use with caution if allergic.
- Qwen2.5 3B `1`: Contains peanuts, for sensitive users please use with caution.
- Qwen2.5 14B (reference, not phone-size): This product contains peanuts. Allergy sufferers should exercise caution.

**10. 每日三次，每次两片，饭后服用。**

- ML Kit (now) `2`: Three times a day, two tablets each, after meals.
- Bergamot (Firefox) `2`: Take it three times a day, two tablets at a time, after a meal.
- Qwen2.5 1.5B `2`: Take three times daily, two tablets each time after meals.
- Qwen2.5 3B `0`: Take twice daily, two tablets each time, after meals.
- Qwen2.5 14B (reference, not phone-size): Take two tablets three times a day after meals.

**11. 退房时间为中午十二点，延迟退房需另付费用。**

- ML Kit (now) `1`: Check-out time is 12 o'clock in noon, delayed check-out needs to pay another fee.
- Bergamot (Firefox) `1`: Check-out time is 12 noon, late check-out is required for an additional fee.
- Qwen2.5 1.5B `2`: Check-out time is 12:00 noon; late check-out is subject to additional fees.
- Qwen2.5 3B `2`: Check-out time is 12 PM, late check-out will incur an additional fee.
- Qwen2.5 14B (reference, not phone-size): Check-out time is 12:00 noon. A fee will be charged for late check-out.

**12. 早餐时间：早上七点至十点，在二楼餐厅。**

- ML Kit (now) `1`: Breakfast time: 7 to ten in the morning, on the second floor.
- Bergamot (Firefox) `2`: Breakfast time: 7 a.m. to 10 a.m. in the restaurant on the second floor.
- Qwen2.5 1.5B `2`: Breakfast hours: 7:00 am to 10:00 am, on the second floor restaurant.
- Qwen2.5 3B `2`: Breakfast time: 7 to 10 AM, second floor restaurant.
- Qwen2.5 14B (reference, not phone-size): Breakfast served from 7:00 AM to 10:00 AM on the second floor dining room.

**13. 您的快递已放在小区门口的丰巢柜，取件码 482913。**

- ML Kit (now) `1`: Your delivery has been placed in the pool cabinet at the door of the community, pick up code 482913.
- Bergamot (Firefox) `1`: Your courier has been placed in the nest cabinet at the door of the community, pick up the code 482913.
- Qwen2.5 1.5B `1`: Your package has been placed at the door of your community's Fuxi cabinet, and the retrieval code is 482913.
- Qwen2.5 3B `1`: Your delivery has been placed at the丰巢柜 downstairs. The pickup code is 482913.
- Qwen2.5 14B (reference, not phone-size): Your快递 has been placed in the Fengchao locker at the entrance of your residential area. Pickup code: 482913.

**14. 师傅，麻烦到前面路口停一下。**

- ML Kit (now) `0`: Master, I am troubled to the front junction.
- Bergamot (Firefox) `1`: Please stop at the front of the road.
- Qwen2.5 1.5B `2`: Sir, could you please stop at the next intersection?
- Qwen2.5 3B `2`: Could you please stop at the next crossing?
- Qwen2.5 14B (reference, not phone-size): Driver, could you please stop at the next intersection?

**15. 这个可以便宜一点吗？我买两个。**

- ML Kit (now) `1`: Can this be cheaper? I bought two.
- Bergamot (Firefox) `1`: Can this be cheaper?I buy two.
- Qwen2.5 1.5B `2`: Is there any discount? I'm buying two.
- Qwen2.5 3B `2`: Can I get it cheaper? I want to buy two.
- Qwen2.5 14B (reference, not phone-size): Can you give me a discount since I'm buying two?

**16. 不好意思，我们今天的菜已经卖完了，明天再来吧。**

- ML Kit (now) `1`: Sorry, our vegetables today have been sold, come back tomorrow.
- Bergamot (Firefox) `0`: Sorry, our food is sold out today, and we’ll be back tomorrow.
- Qwen2.5 1.5B `2`: Sorry, our dishes are sold out today. Come back tomorrow.
- Qwen2.5 3B `2`: We're all out of today’s dishes, come tomorrow please.
- Qwen2.5 14B (reference, not phone-size): Sorry, we have sold out of all our dishes for today. Please come back tomorrow.

**17. 请出示健康码和身份证。**

- ML Kit (now) `2`: Please show your health code and ID card.
- Bergamot (Firefox) `2`: Please show your health code and ID.
- Qwen2.5 1.5B `2`: Please show your health code and ID.
- Qwen2.5 3B `2`: Please show your health code and ID card.
- Qwen2.5 14B (reference, not phone-size): Please show your health code and ID card.

**18. 外国人请到人工窗口办理。**

- ML Kit (now) `1`: Foreigners please go to the artificial window.
- Bergamot (Firefox) `1`: Foreigners are invited to go to the manual window.
- Qwen2.5 1.5B `1`: Please go to the human window for processing.
- Qwen2.5 3B `1`: Foreigners, please go to the counter window for办理.
- Qwen2.5 14B (reference, not phone-size): Foreigners please proceed to the manual service window.

**19. 扫码点餐，餐后付款。**

- ML Kit (now) `0`: Scan code dide, payment post-meals.
- Bergamot (Firefox) `0`: Pay the meal, pay after the meal.
- Qwen2.5 1.5B `2`: Scan to order, pay after meal.
- Qwen2.5 3B `1`: Scan to order food, pay after用餐.
- Qwen2.5 14B (reference, not phone-size): Scan to order, pay after dining.

**20. 空调遥控器在电视柜的抽屉里。**

- ML Kit (now) `2`: Air conditioner remote control in the drawer of the TV cabinet.
- Bergamot (Firefox) `2`: The air conditioning remote control is in the drawer of the TV cabinet.
- Qwen2.5 1.5B `2`: The remote control for the air conditioning is inside the TV cabinet’s drawer.
- Qwen2.5 3B `2`: The remote control for the air conditioner is in the drawer of the TV stand.
- Qwen2.5 14B (reference, not phone-size): The remote control for the air conditioner is in the drawer of the TV cabinet.

**21. 温馨提示：贵重物品请随身携带，丢失概不负责。**

- ML Kit (now) `0`: Tips: Please carry it with you, miss it.
- Bergamot (Firefox) `1`: Tips: Valuable items please carry, lost is not responsible.
- Qwen2.5 1.5B `2`: Please note: Keep valuable items with you at all times as we are not responsible for their loss.
- Qwen2.5 3B `2`: Notice: Please keep your valuables with you and we are not responsible for any loss.
- Qwen2.5 14B (reference, not phone-size): Reminder: Please keep your valuables with you at all times. We are not responsible for any loss.

**22. 明天有大雨，出门记得带伞。**

- ML Kit (now) `1`: There is heavy rain tomorrow, remember to take umbrella.
- Bergamot (Firefox) `1`: It will rain tomorrow, go out and remember to bring an umbrella.
- Qwen2.5 1.5B `2`: It will rain heavily tomorrow; remember to bring an umbrella with you when going out.
- Qwen2.5 3B `2`: It's going to rain heavily tomorrow, remember to bring an umbrella when you go out.
- Qwen2.5 14B (reference, not phone-size): There will be heavy rain tomorrow, remember to bring an umbrella when you go out.

**23. 你好！我是你的房东，wifi密码贴在冰箱上了。**

- ML Kit (now) `2`: Hello! I am your landlord, WiFi password is posted on the refrigerator.
- Bergamot (Firefox) `2`: I'm your landlord, and the wifi password is posted on the fridge.
- Qwen2.5 1.5B `2`: Hi! I'm your landlord; the Wi-Fi password is on the fridge.
- Qwen2.5 3B `2`: Hello! I'm your landlord. The WiFi password is posted on the fridge.
- Qwen2.5 14B (reference, not phone-size): Hello! I am your landlord. The WiFi password is stuck on the fridge.

**24. 银行营业时间：周一至周五 9:00-17:00，节假日休息。**

- ML Kit (now) `1`: Bank business hours: Monday to Friday 9: 00-17: 00, holiday rest.
- Bergamot (Firefox) `2`: Bank opening hours: Monday to Friday 9:00-17:00, holiday rest.
- Qwen2.5 1.5B `2`: Bank operating hours: Monday to Friday from 9:00 am to 5:00 pm, closed on holidays.
- Qwen2.5 3B `2`: Bank operating hours: Monday to Friday, 9:00 AM - 5:00 PM, closed on holidays.
- Qwen2.5 14B (reference, not phone-size): Bank operating hours: 9:00-17:00 Monday to Friday, closed on public holidays.

**25. 地铁2号线换乘请走B出口。**

- ML Kit (now) `1`: Subway Line 2 is transferred to B exit.
- Bergamot (Firefox) `1`: Metro Line 2 please take Exit B.
- Qwen2.5 1.5B `0`: Take the B exit to transfer between Line 2 and Line 4.
- Qwen2.5 3B `1`: Please use the B exit for transferring from Line 2 subway.
- Qwen2.5 14B (reference, not phone-size): To transfer to Line 2, please exit through Exit B.

**26. Where is the nearest subway station?**

- ML Kit (now) `2`: 最近的地铁站在哪里？
- Bergamot (Firefox) `2`: 最近的地铁站在哪里?
- Qwen2.5 1.5B `2`: 最近的地铁站在哪里？
- Qwen2.5 3B `2`: 最近的地铁站在哪里？
- Qwen2.5 14B (reference, not phone-size): 最近的地铁站在哪儿？

**27. I am allergic to peanuts and shellfish.**

- ML Kit (now) `2`: 我对花生和贝类过敏。
- Bergamot (Firefox) `2`: 我对花生和贝类过敏。
- Qwen2.5 1.5B `1`: 我对花生和海鲜过敏。
- Qwen2.5 3B `1`: 我對花生和海鮮過敏。
- Qwen2.5 14B (reference, not phone-size): 我对花生和贝壳类海鲜过敏。

**28. Can I pay by credit card?**

- ML Kit (now) `2`: 我可以用信用卡付款吗？
- Bergamot (Firefox) `2`: 我可以通过信用卡付款吗?
- Qwen2.5 1.5B `2`: 我可以用信用卡付款吗？
- Qwen2.5 3B `2`: 我可以用信用卡支付吗？
- Qwen2.5 14B (reference, not phone-size): 我可以刷信用卡吗？

**29. Please take me to this address.**

- ML Kit (now) `2`: 请带我去这个地址。
- Bergamot (Firefox) `2`: 请带我去这个地址。
- Qwen2.5 1.5B `2`: 请带我去这个地址。
- Qwen2.5 3B `2`: 请带我去这个地址。
- Qwen2.5 14B (reference, not phone-size): 请带我去这个地址。

**30. How much is a ticket to Shanghai?**

- ML Kit (now) `1`: 上海的门票多少钱？
- Bergamot (Firefox) `2`: 去上海的票多少钱?
- Qwen2.5 1.5B `2`: 去上海的票价是多少？
- Qwen2.5 3B `2`: 一张去上海的票多少钱？
- Qwen2.5 14B (reference, not phone-size): 去上海的票多少钱？

**31. I would like to check out tomorrow morning.**

- ML Kit (now) `0`: 我想明天早上留言。
- Bergamot (Firefox) `0`: 我想明天早上去看看。
- Qwen2.5 1.5B `2`: 我想明天早上退房。
- Qwen2.5 3B `1`: 我想明天早上 checkout.
- Qwen2.5 14B (reference, not phone-size): 我想明天早上退房。

**32. The air conditioning in my room is not working.**

- ML Kit (now) `2`: 房间里的空调不起作用。
- Bergamot (Firefox) `1`: 我房间里的空调没有工作。
- Qwen2.5 1.5B `2`: 我的房间的空调不工作了。
- Qwen2.5 3B `2`: 我房间的空调不制冷了。
- Qwen2.5 14B (reference, not phone-size): 我房间的空调坏了。

**33. Is this dish spicy? I can't eat spicy food.**

- ML Kit (now) `1`: 这碟子辣吗？ 我不能吃辛辣的食物。
- Bergamot (Firefox) `1`: 这道菜辣吗? 我吃不上辛辣的食物。
- Qwen2.5 1.5B `2`: 这道菜辣吗？我不吃辣的。
- Qwen2.5 3B `2`: 这道菜辣吗？我不吃辣的。
- Qwen2.5 14B (reference, not phone-size): 这道菜辣吗？我吃不了辣的。

**34. Could you please write it down for me?**

- ML Kit (now) `2`: 你能为我写下来吗？
- Bergamot (Firefox) `1`: 请帮我把它写下来吗?
- Qwen2.5 1.5B `2`: 麻烦你写下来给我。
- Qwen2.5 3B `2`: 你能把这句话写下来吗？
- Qwen2.5 14B (reference, not phone-size): 你能把它写下来给我吗？

**35. I lost my passport. Where is the police station?**

- ML Kit (now) `2`: 我丢失了我的护照。 警察局在哪里？
- Bergamot (Firefox) `1`: 我丢失了护照。 警方在哪里?
- Qwen2.5 1.5B `2`: 我的护照丢了。警察局在哪里？
- Qwen2.5 3B `2`: 我丢了护照，警察局在哪里？
- Qwen2.5 14B (reference, not phone-size): 我把护照丢了。最近的警察局在哪里？

**36. Do you have a room for two nights?**

- ML Kit (now) `2`: 你有两个晚上的房间吗？
- Bergamot (Firefox) `2`: 你有两晚的房间吗?
- Qwen2.5 1.5B `2`: 你有两晚的房间吗？
- Qwen2.5 3B `1`: 你们有一个两个晚上可用的房间吗？
- Qwen2.5 14B (reference, not phone-size): 你有两个晚上的房间吗？

**37. I need a doctor. My friend has a high fever.**

- ML Kit (now) `1`: 我需要一名医生。 我的朋友高烧。
- Bergamot (Firefox) `2`: 我需要一位医生。 我朋友发高烧了。
- Qwen2.5 1.5B `2`: 我需要一个医生。我的朋友发烧了。
- Qwen2.5 3B `2`: 我需要找医生，我朋友发烧了。
- Qwen2.5 14B (reference, not phone-size): 我需要医生。我的朋友发高烧了。

**38. Can you recommend a good local restaurant nearby?**

- ML Kit (now) `1`: 您可以推荐附近的一家良好的当地餐厅吗？
- Bergamot (Firefox) `2`: 你能推荐附近一家不错的本地餐厅吗?
- Qwen2.5 1.5B `2`: 你能推荐一个附近的地道餐厅吗？
- Qwen2.5 3B `2`: 附近有什么推荐的当地餐厅吗？
- Qwen2.5 14B (reference, not phone-size): 你能推荐附近一家好的本地餐馆吗？

**39. What time does the last train leave?**

- ML Kit (now) `2`: 最后一列火车什么时候离开？
- Bergamot (Firefox) `0`: 上一班火车什么时候出发?
- Qwen2.5 1.5B `2`: 最后一班火车几点？
- Qwen2.5 3B `2`: 最后一班火车几点离开？
- Qwen2.5 14B (reference, not phone-size): 最后一班火车是什么时候开？

**40. I'm a vegetarian. No meat, no fish please.**

- ML Kit (now) `1`: 我是素食主义者。 没有肉，请不要鱼。
- Bergamot (Firefox) `2`: 我是个素食者。 不吃肉,不吃鱼。
- Qwen2.5 1.5B `2`: 我是素食者。请不要有肉，也不要有鱼。
- Qwen2.5 3B `2`: 我是素食者，不要肉也不要鱼。
- Qwen2.5 14B (reference, not phone-size): 我是素食者。不要肉，也不要鱼。

**41. Excuse me, is this seat taken?**

- ML Kit (now) `0`: 对不起，这个座位是拍摄的吗？
- Bergamot (Firefox) `0`: 不好意思,这个座位被拿走了吗?
- Qwen2.5 1.5B `2`: 请问，这是空座吗？
- Qwen2.5 3B `2`: 不好意思，这个位子有人坐吗？
- Qwen2.5 14B (reference, not phone-size): 不好意思，这个座位有人吗？

**42. Thank you so much for your help, you are very kind.**

- ML Kit (now) `2`: 非常感谢你的帮助，你很善良。
- Bergamot (Firefox) `2`: 谢谢你的帮助,你非常善良。
- Qwen2.5 1.5B `2`: 非常感谢你的帮助，你真是太好了。
- Qwen2.5 3B `2`: 非常感谢你的帮助，你太好了。
- Qwen2.5 14B (reference, not phone-size): 谢谢你这么帮忙，你真是太好了。

**43. The package should be delivered to the hotel reception.**

- ML Kit (now) `2`: 包裹应送到酒店接待处。
- Bergamot (Firefox) `2`: 包裹应送至酒店前台。
- Qwen2.5 1.5B `2`: 请将包裹送到酒店前台。
- Qwen2.5 3B `2`: 这个包裹应该送到酒店前台。
- Qwen2.5 14B (reference, not phone-size): 这个包裹应该送到酒店前台。

**44. Please turn left at the next traffic light and stop near the bank.**

- ML Kit (now) `2`: 请在下一个红绿灯处左转，停靠在银行附近。
- Bergamot (Firefox) `2`: 请在下一个交通信号灯处左转,在银行附近停车。
- Qwen2.5 1.5B `2`: 请在下个红绿灯处向左转，停在银行附近。
- Qwen2.5 3B `2`: 请在下一个交通灯处左转，停在银行附近。
- Qwen2.5 14B (reference, not phone-size): 请在下一个交通灯处左转，停在银行附近。

**45. Sorry, I don't understand. Could you speak more slowly?**

- ML Kit (now) `2`: 对不起，我不明白。 你能说得更慢吗？
- Bergamot (Firefox) `2`: 对不起,我不明白。 你能说得更慢吗?
- Qwen2.5 1.5B `1`: 对不起，我不明白。你能放慢点吗？
- Qwen2.5 3B `2`: 对不起，我不太明白，请你说话慢一点好吗？
- Qwen2.5 14B (reference, not phone-size): 对不起，我听不懂。你能说慢一点吗？

