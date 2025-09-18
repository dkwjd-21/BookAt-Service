package com.bookat.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
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
	
	// 캡챠 생성
	public static Captcha createCaptcha() {
		return new Captcha.Builder(captcha_width, captcha_height)
				  .addText(new NumbersAnswerProducer(captcha_length))
				  .addNoise().addNoise().addNoise()
				  .addBackground(new GradiatedBackgroundProducer())
				  .build();
	}
	
	// 이미지 데이터를 byte[]로 반환 
	public static byte[] toImageBytes(Captcha captcha) throws IOException {
		// 이미지를 byte[]로 변환하여 반환
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(captcha.getImage(), "png", baos);
		return baos.toByteArray();
	}
	
	// 오디오 데이터를 byte로 변환 
	public static byte[] toAudioBytes(String answer) throws IOException {
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
