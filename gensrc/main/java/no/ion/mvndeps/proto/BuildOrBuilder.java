// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: builds.proto

package no.ion.mvndeps.proto;

public interface BuildOrBuilder extends
    // @@protoc_insertion_point(interface_extends:no.ion.mvndeps.proto.Build)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string groupId = 1;</code>
   */
  java.lang.String getGroupId();
  /**
   * <code>string groupId = 1;</code>
   */
  com.google.protobuf.ByteString
      getGroupIdBytes();

  /**
   * <code>string artifactId = 2;</code>
   */
  java.lang.String getArtifactId();
  /**
   * <code>string artifactId = 2;</code>
   */
  com.google.protobuf.ByteString
      getArtifactIdBytes();

  /**
   * <code>string path = 3;</code>
   */
  java.lang.String getPath();
  /**
   * <code>string path = 3;</code>
   */
  com.google.protobuf.ByteString
      getPathBytes();

  /**
   * <code>sfixed64 buildTimeNanos = 4;</code>
   */
  long getBuildTimeNanos();
}
