package com.mindtalk.forum.modules.post.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mindtalk.forum.modules.post.entity.Post;
import com.mindtalk.forum.modules.post.mapper.PostMapper;
import com.mindtalk.forum.modules.user.entity.User;
import com.mindtalk.forum.modules.user.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "RSS 订阅")
@RestController
@RequestMapping("/rss")
@RequiredArgsConstructor
public class RssController {

    private final PostMapper postMapper;
    private final UserMapper userMapper;

    private static final DateTimeFormatter RFC822 = DateTimeFormatter.RFC_1123_DATE_TIME;

    @Value("${rss.title:MindTalk 最新帖子}")
    private String rssTitle;

    @Value("${rss.description:MindTalk 思享论坛最新帖子}")
    private String rssDescription;

    @Value("${rss.generator:MindTalk Forum}")
    private String rssGenerator;

    @Value("${rss.site-url:https://mindtalk.example.com}")
    private String rssSiteUrl;

    @Value("${rss.timezone:Asia/Shanghai}")
    private String rssTimezone;

    @Value("${rss.default-limit:20}")
    private int rssDefaultLimit;

    @Value("${rss.truncate-length:200}")
    private int rssTruncateLength;

    @Value("${feature.rss.enabled:true}")
    private boolean rssEnabled;

    @Operation(summary = "最新帖子 RSS")
    @GetMapping(value = "/posts", produces = MediaType.APPLICATION_XML_VALUE)
    public String postsFeed() {
        if (!rssEnabled) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1).orderByDesc(Post::getCreateTime).last("LIMIT " + rssDefaultLimit);
        List<Post> posts = postMapper.selectList(wrapper);
        return buildRss(rssTitle, rssDescription, "/posts", posts);
    }

    @Operation(summary = "分类 RSS")
    @GetMapping(value = "/category/{id}", produces = MediaType.APPLICATION_XML_VALUE)
    public String categoryFeed(@PathVariable Long id) {
        if (!rssEnabled) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getCategoryId, id).eq(Post::getStatus, 1)
                .orderByDesc(Post::getCreateTime).last("LIMIT " + rssDefaultLimit);
        List<Post> posts = postMapper.selectList(wrapper);
        return buildRss("MindTalk 分类帖子", "分类帖子订阅", "/posts?categoryId=" + id, posts);
    }

    @Operation(summary = "用户 RSS")
    @GetMapping(value = "/user/{id}", produces = MediaType.APPLICATION_XML_VALUE)
    public String userFeed(@PathVariable Long id) {
        if (!rssEnabled) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
        User user = userMapper.selectById(id);
        String userName = user != null ? user.getNickname() : "用户";
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getAuthorId, id).eq(Post::getStatus, 1)
                .orderByDesc(Post::getCreateTime).last("LIMIT " + rssDefaultLimit);
        List<Post> posts = postMapper.selectList(wrapper);
        return buildRss(userName + " 的帖子", userName + " 发布的帖子", "/users/" + id, posts);
    }

    private String buildRss(String title, String description, String link, List<Post> posts) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<rss version=\"2.0\"><channel>");
        xml.append("<title>").append(escape(title)).append("</title>");
        xml.append("<description>").append(escape(description)).append("</description>");
        xml.append("<link>").append(rssSiteUrl).append(link).append("</link>");
        xml.append("<generator>").append(escape(rssGenerator)).append("</generator>");

        for (Post p : posts) {
            xml.append("<item>");
            xml.append("<title>").append(escape(p.getTitle())).append("</title>");
            xml.append("<link>").append(rssSiteUrl).append("/posts/").append(p.getId()).append("</link>");
            xml.append("<guid>").append(rssSiteUrl).append("/posts/").append(p.getId()).append("</guid>");
            xml.append("<description>").append(escape(p.getContentText() != null ? truncate(p.getContentText(), rssTruncateLength) : "")).append("</description>");
            if (p.getCreateTime() != null) {
                xml.append("<pubDate>").append(p.getCreateTime().atZone(ZoneId.of(rssTimezone)).format(RFC822)).append("</pubDate>");
            }
            xml.append("</item>");
        }

        xml.append("</channel></rss>");
        return xml.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
