package com.bookat.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import jakarta.servlet.http.HttpSession;
import nl.captcha.Captcha;
import nl.captcha.audio.AudioCaptcha;
import nl.captcha.backgrounds.GradiatedBackgroundProducer;
import nl.captcha.text.producer.NumbersAnswerProducer;
import nl.captcha.text.producer.TextProducer;

public class CaptchaUtil {
	// 보안문자 이미지 가로 & 세로 
	private static final int captcha_width = 200;
	private static final int captcha_height = 60;
	// 보안문자 길이 
	private static final int captcha_length = 6;
	
	// 캡챠 이미지를 생성하고 세션에 저장한 뒤, 이미지 데이터를 byte[]로 반환 
	public static byte[] generateImage(HttpSession session) throws IOException {
		// 캡챠 생성
		Captcha captcha = new Captcha.Builder(captcha_width, captcha_height)
							  .addText(new NumbersAnswerProducer(captcha_length))
							  .addNoise().addNoise().addNoise()
							  .addBackground(new GradiatedBackgroundProducer())
							  .build();
		
		// 세션에 저장
		session.setAttribute(Captcha.NAME, captcha);
		
		// 이미지를 byte[]로 변환하여 반환
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(captcha.getImage(), "png", baos);
		return baos.toByteArray();
	}
	
	// 세션에 저장된 캡챠의 정답으로 오디오 데이터를 생성 -> byte[]로 반환 
	public static byte[] generateAudio(HttpSession session) throws IOException {
		Captcha captcha = (Captcha) session.getAttribute(Captcha.NAME);
		
		if(captcha == null) {
			throw new RuntimeException("세션에 캡챠가 없습니다.");
		}
		
		String answer = captcha.getAnswer();
		
		TextProducer textProducer = () -> answer;
		
		AudioCaptcha audioCaptcha = new AudioCaptcha.Builder()
										.addAnswer(textProducer)
										.addNoise().build();
		
		// 오디오 데이터를 byte로 변환 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		javax.sound.sampled.AudioSystem.write(
							audioCaptcha.getChallenge().getAudioInputStream(), 
							javax.sound.sampled.AudioFileFormat.Type.WAVE,
							baos);
		
		return baos.toByteArray();
	}
	
}
